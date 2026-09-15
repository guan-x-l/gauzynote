import {expect, test} from '@playwright/test'

test('an unauthenticated mobile note link returns to the mobile login page', async ({page}) => {
    await page.goto('/gauzynote/m/note/42')
    await expect(page).toHaveURL(/\/gauzynote\/m\/login\?redirect=\/m\/note\/42/)
    await expect(page.getByRole('heading', {name: /登陆|sign in/i})).toBeVisible()
})

test('a long mobile reading view scrolls inside the note content area', async ({page, context}) => {
    await context.addCookies([{name: 'Auth-Token', value: 'test-token', url: 'http://127.0.0.1:4173'}])
    await page.route('**/getInfo', route => route.fulfill({json: {
        code: 200,
        data: {username: 'tester'},
        isDefaultModifyPwd: false
    }}))
    await page.route('**/asset/note/42', route => route.fulfill({json: {
        code: 200,
        data: {
            noteName: 'Long note',
            content: Array.from({length: 80}, (_, index) => `## Section ${index}\nLong preview text.`).join('\n\n')
        }
    }}))

    await page.goto('/gauzynote/m/note/42')
    await page.getByRole('button', {name: /编辑|edit/i}).click()
    await expect(page.locator('.mobile-note__editor')).toBeVisible()
    await page.getByRole('button', {name: /阅读|read/i}).click()
    await expect(page.locator('.mobile-note__editor')).not.toBeVisible()
    await expect(page.locator('.mobile-markdown-body')).toContainText('Section 0')

    const metrics = await page.locator('.mobile-note__body').evaluate(element => {
        element.scrollTop = 240
        return {clientHeight: element.clientHeight, scrollHeight: element.scrollHeight, scrollTop: element.scrollTop}
    })
    expect(metrics.scrollHeight).toBeGreaterThan(metrics.clientHeight)
    expect(metrics.scrollTop).toBeGreaterThan(0)
})

test('the mobile directory shows its path and resource SVG icons', async ({page, context}) => {
    await context.addCookies([{name: 'Auth-Token', value: 'test-token', url: 'http://127.0.0.1:4173'}])
    await page.route('**/getInfo', route => route.fulfill({json: {
        code: 200,
        data: {username: 'tester'},
        isDefaultModifyPwd: false
    }}))
    await page.route('**/system/resourceNode', route => route.fulfill({json: {
        code: 200,
        data: [
            {nodeId: 1, parentId: null, nodeType: '1', nodeName: 'Project'},
            {nodeId: 2, parentId: 1, nodeType: '2', nodeName: 'Plan', relatedId: 42}
        ]
    }}))

    await page.goto('/gauzynote/m')
    const tabButtons = page.locator('.mobile-home__tabbar button')
    await expect(tabButtons.nth(0).locator('.icon-folder')).toBeVisible()
    await expect(tabButtons.nth(1).locator('.icon-user')).toBeVisible()
    const folderRow = page.locator('.mobile-row').filter({hasText: 'Project'})
    await expect(folderRow.locator('.icon-folder')).toBeVisible()
    await folderRow.click()

    await expect(page.locator('.mobile-row--back')).toContainText(/(目录|Directory) \/ Project/)
    await expect(page.locator('.mobile-row--back .icon-folder')).toBeVisible()
    await expect(page.locator('.mobile-row').filter({hasText: 'Plan'}).locator('.icon-file-text')).toBeVisible()
})
