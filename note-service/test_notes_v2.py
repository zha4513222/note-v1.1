"""
Playwright 测试脚本 - 笔记管理系统 (改进版)
测试: 笔记列表、创建笔记、搜索、标签功能、统计页面
"""
import asyncio
import os
from playwright.async_api import async_playwright

FRONTEND_URL = "http://localhost:5174"
BACKEND_URL = "http://localhost:8082"
SCREENSHOT_DIR = "/tmp"

class NoteAppTester:
    def __init__(self):
        self.results = []
        self.browser = None
        self.context = None
        self.page = None
        self.bugs = []

    def log(self, test_name, status, message=""):
        """记录测试结果"""
        result = f"[{'PASS' if status else 'FAIL'}] {test_name}"
        if message:
            result += f" - {message}"
        self.results.append((status, result))
        print(result)

    def bug_report(self, description, severity="medium"):
        """记录发现的bug"""
        bug = f"[BUG-{severity.upper()}] {description}"
        self.bugs.append(bug)
        print(f"  {bug}")

    async def setup(self):
        """初始化浏览器"""
        playwright = await async_playwright().start()
        self.browser = await playwright.chromium.launch(headless=True)
        self.context = await self.browser.new_context(viewport={"width": 1280, "height": 720})
        self.page = await self.context.new_page()
        self.page.set_default_timeout(15000)

    async def take_screenshot(self, name):
        """截图并保存"""
        path = os.path.join(SCREENSHOT_DIR, f"{name}.png")
        await self.page.screenshot(path=path, full_page=True)
        print(f"截图已保存: {path}")
        return path

    async def teardown(self):
        """关闭浏览器"""
        if self.browser:
            await self.browser.close()

    async def wait_for_network_idle(self):
        """等待网络空闲"""
        await self.page.wait_for_load_state("networkidle")

    async def test_homepage_loads(self):
        """测试1: 首页/笔记列表页面加载"""
        try:
            await self.page.goto(FRONTEND_URL, wait_until="networkidle")
            await self.wait_for_network_idle()
            await self.take_screenshot("01_homepage")

            title = await self.page.title()
            self.log("首页加载", True, f"标题: {title}")

            # 检查主要元素
            body = self.page.locator("body")
            if await body.count() > 0:
                self.log("页面内容", True, "页面成功加载")

            # 检查侧边栏菜单
            menu_items = self.page.locator(".el-menu-item, .el-sub-menu")
            menu_count = await menu_items.count()
            if menu_count > 0:
                self.log("侧边栏菜单", True, f"找到 {menu_count} 个菜单项")

        except Exception as e:
            await self.take_screenshot("01_homepage_error")
            self.log("首页加载", False, str(e))

    async def test_create_note(self):
        """测试2: 创建新笔记"""
        try:
            await self.page.goto(FRONTEND_URL, wait_until="networkidle")
            await self.wait_for_network_idle()
            await self.take_screenshot("02_before_create")

            # 查找主创建按钮 (在页面右侧或工具栏)
            # Element Plus 按钮通常有 .el-button 类
            create_btn = self.page.locator(".el-button--primary").first

            if await create_btn.count() > 0:
                btn_text = await create_btn.text_content()
                print(f"找到主按钮: {btn_text}")
                await create_btn.click()
                await asyncio.sleep(1)
                await self.take_screenshot("02_modal_opened")

                # 检查是否出现模态框
                modal = self.page.locator(".el-dialog, .el-drawer")
                if await modal.count() > 0:
                    self.log("创建笔记模态框", True, "模态框已打开")

                    # 查找表单输入框
                    title_input = self.page.locator(".el-dialog input").first
                    if await title_input.count() == 0:
                        title_input = self.page.locator("input[placeholder*='标题']")

                    content_input = self.page.locator("textarea").first

                    if await title_input.count() > 0:
                        await title_input.fill("Playwright测试笔记")
                        self.log("填写标题", True)

                    if await content_input.count() > 0:
                        await content_input.fill("这是通过Playwright自动测试创建的笔记内容")
                        self.log("填写内容", True)

                    await self.take_screenshot("02_form_filled")

                    # 点击创建按钮
                    dialog_create_btn = self.page.locator(".el-dialog .el-button--primary")
                    if await dialog_create_btn.count() > 0:
                        await dialog_create_btn.click()
                        await self.wait_for_network_idle()
                        await self.take_screenshot("02_after_create")
                        self.log("创建笔记", True)
                    else:
                        self.log("创建笔记", False, "未找到模态框中的创建按钮")
                else:
                    self.log("创建笔记", False, "模态框未打开")
            else:
                self.log("创建笔记", False, "未找到创建按钮")

        except Exception as e:
            await self.take_screenshot("02_create_error")
            self.log("创建笔记", False, str(e))

    async def test_search_functionality(self):
        """测试3: 搜索功能"""
        try:
            await self.page.goto(FRONTEND_URL, wait_until="networkidle")
            await self.wait_for_network_idle()
            await self.take_screenshot("03_search_page")

            # 查找搜索框 - Element Plus 通常有 placeholder
            search_input = self.page.locator("input[placeholder*='搜索'], input[placeholder*='搜索']").first

            if await search_input.count() == 0:
                # 尝试查找所有 input 并过滤
                all_inputs = self.page.locator("input")
                for i in range(await all_inputs.count()):
                    input_el = all_inputs.nth(i)
                    placeholder = await input_el.get_attribute("placeholder")
                    if placeholder and "搜索" in placeholder:
                        search_input = input_el
                        break

            if await search_input.count() > 0:
                await search_input.fill("测试")
                await asyncio.sleep(1)
                await self.take_screenshot("03_search_results")
                self.log("搜索功能", True, "搜索框可用")
            else:
                # 可能是搜索图标按钮
                search_btn = self.page.locator(".el-input__suffix-icon")
                if await search_btn.count() > 0:
                    await search_btn.first.click()
                    await asyncio.sleep(0.5)
                    search_input = self.page.locator(".el-input__inner")
                    if await search_input.count() > 0:
                        await search_input.first.fill("测试")
                        await self.take_screenshot("03_search_results")
                        self.log("搜索功能", True, "搜索框可用")
                    else:
                        self.log("搜索功能", False, "未找到搜索输入框")
                else:
                    self.log("搜索功能", False, "未找到搜索框")

        except Exception as e:
            await self.take_screenshot("03_search_error")
            self.log("搜索功能", False, str(e))

    async def test_tags_chinese(self):
        """测试4: 标签功能（特别是中文标签）"""
        try:
            await self.page.goto(f"{FRONTEND_URL}/#/tags", wait_until="networkidle")
            await asyncio.sleep(2)
            await self.take_screenshot("04_tags_page")

            # 检查页面是否正确加载
            page_text = await self.page.text_content("body")
            if "标签" in page_text:
                self.log("标签页面加载", True)

            # 查找添加按钮
            add_btn = self.page.locator("button:has-text('添加'), .el-button:has-text('添加')").first

            if await add_btn.count() > 0:
                await add_btn.click()
                await asyncio.sleep(1)
                await self.take_screenshot("04_tags_add_dialog")

                # 在对话框中输入中文标签
                dialog_input = self.page.locator(".el-dialog input, .el-drawer input").first
                if await dialog_input.count() > 0:
                    await dialog_input.fill("测试标签")
                    await self.take_screenshot("04_tags_chinese_input")

                    # 点击确定
                    confirm_btn = self.page.locator(".el-dialog .el-button--primary, .el-drawer .el-button--primary")
                    if await confirm_btn.count() > 0:
                        await confirm_btn.click()
                        await self.wait_for_network_idle()
                        await self.take_screenshot("04_tags_after_create")
                        self.log("创建中文标签", True)
                    else:
                        self.log("创建中文标签", False, "未找到确定按钮")
                else:
                    self.log("创建中文标签", False, "未找到标签输入框")
            else:
                # 可能需要通过右键菜单或其他方式添加
                self.log("标签功能", False, "未找到添加按钮")

        except Exception as e:
            await self.take_screenshot("04_tags_error")
            self.log("标签功能", False, str(e))

    async def test_statistics_page(self):
        """测试5: 统计页面"""
        try:
            await self.page.goto(f"{FRONTEND_URL}/#/statistics", wait_until="networkidle")
            await asyncio.sleep(2)
            await self.take_screenshot("05_statistics_page")

            # 检查页面内容
            page_text = await self.page.text_content("body")
            if "统计" in page_text:
                self.log("统计页面加载", True)

            # 检查图表区域是否有数据
            no_data_elements = self.page.locator("text=暂无数据")
            no_data_count = await no_data_elements.count()

            if no_data_count > 0:
                self.bug_report("统计页面存在无数据的图表区域", "medium")
                self.log("统计页面", True, f"页面加载，但有 {no_data_count} 个图表显示'暂无数据'")
            else:
                self.log("统计页面", True, "所有图表都有数据")

            # 检查统计元素
            stats_elements = self.page.locator("[class*='statistic'], [class*='chart']")
            if await stats_elements.count() > 0:
                self.log("统计图表", True, f"找到 {await stats_elements.count()} 个图表")

        except Exception as e:
            await self.take_screenshot("05_statistics_error")
            self.log("统计页面", False, str(e))

    async def test_backend_api(self):
        """测试6: 后端API可用性"""
        try:
            # 测试 tags API
            response = await self.page.request.get(f"{BACKEND_URL}/api/tags")
            if response.status == 200:
                data = await response.json()
                self.log("后端API - /api/tags", True, f"返回 {len(data) if isinstance(data, list) else 'data'} 条记录")
            else:
                self.log("后端API - /api/tags", False, f"状态码: {response.status}")

            # 测试 notes API
            response = await self.page.request.get(f"{BACKEND_URL}/api/notes")
            if response.status == 200:
                self.log("后端API - /api/notes", True)
            else:
                self.log("后端API - /api/notes", False, f"状态码: {response.status}")

            # 测试其他API
            response = await self.page.request.get(f"{BACKEND_URL}/api/statistics")
            if response.status == 200:
                self.log("后端API - /api/statistics", True)
            else:
                self.log("后端API - /api/statistics", False, f"状态码: {response.status}")

        except Exception as e:
            self.log("后端API", False, str(e))

    async def run_all_tests(self):
        """运行所有测试"""
        print("=" * 60)
        print("Start Note Management System Tests")
        print("=" * 60)

        await self.setup()

        try:
            await self.test_homepage_loads()
            await self.test_create_note()
            await self.test_search_functionality()
            await self.test_tags_chinese()
            await self.test_statistics_page()
            await self.test_backend_api()
        finally:
            await self.teardown()

        # 打印测试报告
        print("\n" + "=" * 60)
        print("TEST REPORT")
        print("=" * 60)
        passed = sum(1 for s, _ in self.results if s)
        total = len(self.results)
        print(f"Passed: {passed}/{total}")

        for status, result in self.results:
            print(result)

        if self.bugs:
            print("\n" + "=" * 60)
            print("BUGS FOUND")
            print("=" * 60)
            for bug in self.bugs:
                print(bug)

        print("=" * 60)
        print(f"Screenshots saved to: {SCREENSHOT_DIR}")
        print("=" * 60)

        return self.results, self.bugs


async def main():
    tester = NoteAppTester()
    results, bugs = await tester.run_all_tests()


if __name__ == "__main__":
    asyncio.run(main())
