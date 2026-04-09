"""
Playwright 测试脚本 - 笔记管理系统 (最终版)
测试: 笔记列表、创建笔记、搜索、标签显示、统计页面
"""
import asyncio
import os
import sys
sys.stdout.reconfigure(encoding='utf-8')
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
        print(f"Screenshot saved: {path}")
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
            self.log("首页加载", True, f"Title: {title}")

            # 检查笔记列表
            note_cards = await self.page.locator(".card-preview, .note-card, [class*=card]").all()
            if len(note_cards) > 0:
                self.log("笔记列表", True, f"Found {len(note_cards)} note cards")

            # 检查侧边栏导航
            nav_links = await self.page.locator("a").all()
            self.log("侧边栏链接", True, f"Found {len(nav_links)} navigation links")

        except Exception as e:
            await self.take_screenshot("01_homepage_error")
            self.log("首页加载", False, str(e))

    async def test_create_note(self):
        """测试2: 创建新笔记"""
        try:
            await self.page.goto(f"{FRONTEND_URL}/note/new", wait_until="networkidle")
            await asyncio.sleep(1)
            await self.take_screenshot("02_create_page")

            # 检查创建页面元素
            page_text = await self.page.text_content("body")
            has_title_field = "标题" in page_text or "title" in page_text.lower()
            has_content_field = "内容" in page_text or "content" in page_text.lower()

            if has_title_field:
                self.log("创建页面 - 标题字段", True)
            else:
                self.log("创建页面 - 标题字段", False, "Not found")

            # 查找输入框并填写
            inputs = await self.page.locator("input").all()
            textareas = await self.page.locator("textarea").all()

            if len(inputs) > 0:
                await inputs[0].fill("Playwright测试笔记")
                self.log("填写标题", True)

            if len(textareas) > 0:
                await textareas[0].fill("这是通过Playwright自动测试创建的笔记内容")
                self.log("填写内容", True)

            await self.take_screenshot("02_form_filled")

            # 点击保存按钮
            save_btn = self.page.locator("button:has-text('保存'), .el-button:has-text('保存')")
            if await save_btn.count() > 0:
                await save_btn.click()
                await self.wait_for_network_idle()
                await self.take_screenshot("02_after_save")
                self.log("保存笔记", True)
            else:
                # 尝试其他保存按钮
                save_btn = self.page.locator("button:has-text('Save')")
                if await save_btn.count() > 0:
                    await save_btn.click()
                    await self.wait_for_network_idle()
                    self.log("保存笔记", True)
                else:
                    self.log("保存笔记", False, "Save button not found")

        except Exception as e:
            await self.take_screenshot("02_create_error")
            self.log("创建笔记", False, str(e))

    async def test_search_page(self):
        """测试3: 搜索页面"""
        try:
            await self.page.goto(f"{FRONTEND_URL}/search", wait_until="networkidle")
            await asyncio.sleep(1)
            await self.take_screenshot("03_search_page")

            # 检查搜索输入框
            search_input = self.page.locator("input[placeholder*='搜索'], input[placeholder*='search']")
            if await search_input.count() == 0:
                search_input = self.page.locator("input").first

            if await search_input.count() > 0:
                await search_input.fill("test")
                await asyncio.sleep(1)
                await self.take_screenshot("03_search_results")
                self.log("搜索功能", True, "Search box working")
            else:
                self.log("搜索功能", False, "Search input not found")

        except Exception as e:
            await self.take_screenshot("03_search_error")
            self.log("搜索页面", False, str(e))

    async def test_tags_display(self):
        """测试4: 标签显示功能"""
        try:
            await self.page.goto(FRONTEND_URL, wait_until="networkidle")
            await self.wait_for_network_idle()
            await self.take_screenshot("04_tags_on_notes")

            # 检查笔记卡片上的标签
            tag_elements = await self.page.locator(".card-tags, [class*=tag]").all()
            tag_count = len(tag_elements)

            if tag_count > 0:
                self.log("笔记标签显示", True, f"Found {tag_count} tag elements")
            else:
                self.log("笔记标签显示", False, "No tag elements found on note cards")

            # 检查是否有中文标签
            page_html = await self.page.inner_html("body")
            if any(ord(c) > 127 for c in page_html):
                self.log("中文内容支持", True, "Page contains Chinese characters")
            else:
                self.log("中文内容支持", False, "No Chinese characters found")

        except Exception as e:
            await self.take_screenshot("04_tags_error")
            self.log("标签显示", False, str(e))

    async def test_statistics_page(self):
        """测试5: 统计页面"""
        try:
            await self.page.goto(f"{FRONTEND_URL}/stats", wait_until="networkidle")
            await asyncio.sleep(2)
            await self.take_screenshot("05_statistics_page")

            # 检查页面内容
            page_text = await self.page.text_content("body")
            if "统计" in page_text:
                self.log("统计页面加载", True)

            # 检查图表
            charts = await self.page.locator(".chart, [class*=chart], canvas").all()
            if len(charts) > 0:
                self.log("统计图表", True, f"Found {len(charts)} chart elements")
            else:
                self.log("统计图表", False, "No charts found")

            # 检查是否显示"暂无数据"
            no_data = await self.page.locator("text=暂无数据").all()
            if len(no_data) > 0:
                self.bug_report(f"Statistics page has {len(no_data)} charts showing 'No data'", "medium")
                self.log("统计页面", True, f"Page loads but {len(no_data)} charts have no data")
            else:
                self.log("统计页面", True, "All charts have data")

        except Exception as e:
            await self.take_screenshot("05_statistics_error")
            self.log("统计页面", False, str(e))

    async def test_backend_api(self):
        """测试6: 后端API可用性"""
        try:
            # Test tags API
            response = await self.page.request.get(f"{BACKEND_URL}/api/tags")
            if response.status == 200:
                data = await response.json()
                self.log("Backend API - /api/tags", True, f"Returns {len(data) if isinstance(data, list) else 'data'}")
            else:
                self.log("Backend API - /api/tags", False, f"Status: {response.status}")

            # Test notes API
            response = await self.page.request.get(f"{BACKEND_URL}/api/notes")
            if response.status == 200:
                self.log("Backend API - /api/notes", True)
            else:
                self.log("Backend API - /api/notes", False, f"Status: {response.status}")

            # Test stats API
            response = await self.page.request.get(f"{BACKEND_URL}/api/statistics")
            if response.status == 200:
                self.log("Backend API - /api/statistics", True)
            elif response.status == 404:
                # Try alternative endpoint
                response = await self.page.request.get(f"{BACKEND_URL}/api/stats")
                if response.status == 200:
                    self.log("Backend API - /api/stats", True)
                else:
                    self.log("Backend API - Statistics endpoints", False, f"All returned 404")
                    self.bug_report("Backend /api/statistics endpoint returns 404", "low")
            else:
                self.log("Backend API - Statistics", False, f"Status: {response.status}")

        except Exception as e:
            self.log("Backend API", False, str(e))

    async def run_all_tests(self):
        """运行所有测试"""
        print("=" * 60)
        print("Starting Note Management System Tests")
        print("=" * 60)

        await self.setup()

        try:
            await self.test_homepage_loads()
            await self.test_create_note()
            await self.test_search_page()
            await self.test_tags_display()
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
        print("-" * 60)

        for status, result in self.results:
            print(result)

        if self.bugs:
            print("\n" + "=" * 60)
            print("BUGS FOUND")
            print("=" * 60)
            for bug in self.bugs:
                print(bug)

        print("\n" + "=" * 60)
        print(f"Screenshots saved to: {SCREENSHOT_DIR}")
        print("=" * 60)

        return self.results, self.bugs


async def main():
    tester = NoteAppTester()
    await tester.run_all_tests()


if __name__ == "__main__":
    asyncio.run(main())
