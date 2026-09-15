import {describe, expect, it} from 'vitest'
import {isCurrentSnapshot, isMobilePath, resolveMobileRedirect} from './mobile.js'

describe('mobile navigation helpers', () => {
    it('keeps only application-owned mobile login returns', () => {
        expect(resolveMobileRedirect('/m')).toBe('/m')
        expect(resolveMobileRedirect('/m/note/42')).toBe('/m/note/42')
        expect(resolveMobileRedirect('/m/note/42?from=home')).toBe('/m/note/42?from=home')
        expect(resolveMobileRedirect('/login')).toBe('/m')
        expect(resolveMobileRedirect('https://example.com')).toBe('/m')
        expect(isMobilePath('/m/note/42')).toBe(true)
        expect(isMobilePath('/note/42')).toBe(false)
    })
})

describe('mobile save snapshots', () => {
    it('does not let an older save clear later input', () => {
        const submitted = {noteId: '42', noteName: 'Before', content: 'one'}
        expect(isCurrentSnapshot(submitted, {noteId: '42', noteName: 'Before', content: 'two'})).toBe(false)
        expect(isCurrentSnapshot(submitted, submitted)).toBe(true)
        expect(isCurrentSnapshot(submitted, {...submitted, noteId: '43'})).toBe(false)
    })
})
