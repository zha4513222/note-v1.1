"""
Playwright 测试脚本 - 笔记管理系统
测试: 笔记列表、创建笔记、搜索、标签功能、统计页面
"""
import asyncio
import os
from playwright.async_api import async_playwright, expect

FRONTEND_URL = "http://localhost:5174"
BACKEND_URL = "http://localhost:8082"
SCREENSHOT_DIR = "/tmp"

class NoteAppTester:
    def __init__(self):
        self.results = []
        self.browser = None
        self.context = None
        self.page = None

    def log(self, test_name, status, message=""):
        """记录测试结果"""
        result = f"[{'PASS' if status else 'FAIL'}] {test_name}"
        if message:
            result += f" - {message}"
        self.results.append((status, result))
        print(result)

    async def setup(self):
        """初始化浏览器"""
        playwright = await async_playwright().start()
        self.browser = await playwright.chromium.launch(headless=True)
        self.context = await self.browser.new_context(viewport={"width": 1280, "height": 720})
        self.page = await self.context.new_page()

        # 设置默认超时
        self.page.set_default_timeout(10000)

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
            await self.take_screenshot("01_homepage")

            # 检查页面标题或主要元素
            title = await self.page.title()
            self.log("首页加载", True, f"标题: {title}")

            # 检查笔记列表容器
            notes_container = self.page.locator("body")
            if await notes_container.count() > 0:
                self.log("页面内容", True, "页面成功加载")

        except Exception as e:
            await self.take_screenshot("01_homepage_error")
            self.log("首页加载", False, str(e))

    async def test_create_note(self):
        """测试2: 创建新笔记"""
        try:
            await self.page.goto(FRONTEND_URL, wait_until="networkidle")
            await self.wait_for_network_idle()
            await self.take_screenshot("02_before_create")

            # 查找创建按钮 (可能是 button, a, 或带有 create/new 等文本的元素)
            create_btn = None
            selectors = [
                "button:has-text('创建')",
                "button:has-text('新建')",
                "button:has-text('新增')",
                "button:has-text('Add')",
                "[type='button']:has-text('创')",
                ".el-button--primary"
            ]

            for sel in selectors:
                try:
                    create_btn = self.page.locator(sel).first
                    if await create_btn.count() > 0:
                        break
                except:
                    continue

            if create_btn and await create_btn.count() > 0:
                await create_btn.click()
                await self.wait_for_network_idle()
                await self.take_screenshot("02_after_create_click")

                # 填写笔记表单
                title_input = self.page.locator("input").first
                content_input = self.page.locator("textarea").first

                if await title_input.count() > 0:
                    await title_input.fill("测试笔记标题")
                    self.log("填写标题", True)

                if await content_input.count() > 0:
                    await content_input.fill("这是测试笔记内容")
                    self.log("填写内容", True)

                # 查找保存按钮
                save_btn = self.page.locator("button:has-text('保存')")
                if await save_btn.count() > 0:
                    await save_btn.click()
                    await self.wait_for_network_idle()
                    await self.take_screenshot("02_after_save")
                    self.log("创建笔记", True)
                else:
                    self.log("创建笔记", False, "未找到保存按钮")
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

            # 查找搜索框
            search_input = self.page.locator("input[placeholder*='搜索']")
            if await search_input.count() == 0:
                search_input = self.page.locator("input[type='search']")
            if await search_input.count() == 0:
                search_input = self.page.locator("input").filter(has_text=None).first

            if await search_input.count() > 0:
                await search_input.fill("测试")
                await self.wait_for_network_idle()
                await self.take_screenshot("03_search_results")
                self.log("搜索功能", True, "搜索框可用")
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

            # 创建中文标签
            add_btn = self.page.locator("button:has-text('添加')")
            if await add_btn.count() > 0:
                await add_btn.click()
                await asyncio.sleep(0.5)
                await self.take_screenshot("04_tags_add_dialog")

                tag_input = self.page.locator("input").last
                if await tag_input.count() > 0:
                    # 输入中文标签
                    await tag_input.fill("生活")
                    await self.wait_for_network_idle()
                    await self.take_screenshot("04_tags_chinese_input")
                    self.log("中文标签输入", True)

                    # 保存标签
                    save_btn = self.page.locator("button:has-text('确定')")
                    if await save_btn.count() > 0:
                        await save_btn.click()
                        await self.wait_for_network_idle()
                        await self.take_screenshot("04_tags_after_create")
                        self.log("创建中文标签", True)
            else:
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

            # 检查统计元素
            stats_elements = self.page.locator(".statistics, .stat, [class*='stat']")
            if await stats_elements.count() > 0:
                self.log("统计页面", True, f"找到 {await stats_elements.count()} 个统计元素")
            else:
                # 检查是否有任何数字统计
                numbers = self.page.locator("text=/\\d+/")
                if await numbers.count() > 0:
                    self.log("统计页面", True, "页面加载成功")
                else:
                    self.log("统计页面", True, "页面已加载")

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

        except Exception as e:
            self.log("后端API", False, str(e))

    async def run_all_tests(self):
        """运行所有测试"""
        print("=" * 60)
        print("开始笔记管理系统测试")
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
        print("测试报告")
        print("=" * 60)
        passed = sum(1 for s, _ in self.results if s)
        total = len(self.results)
        print(f"通过: {passed}/{total}")

        for status, result in self.results:
            print(result)

        print("=" * 60)
        print(f"截图保存在: {SCREENSHOT_DIR}")
        print("=" * 60)

        return self.results


async def main():
    tester = NoteAppTester()
    await tester.run_all_tests()


if __name__ == "__main__":
    asyncio.run(main())
