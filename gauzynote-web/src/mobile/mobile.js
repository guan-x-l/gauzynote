export function isMobilePath(path) {
    return path === '/m' || path.startsWith('/m/')
}

export function resolveMobileRedirect(path) {
    if (path === '/m' || /^\/m\/note\/[^/?#]+(?:\?[^#]*)?$/.test(path || '')) {
        return path
    }
    return '/m'
}

export function isCurrentSnapshot(snapshot, current) {
    return snapshot.noteId === current.noteId
        && snapshot.noteName === current.noteName
        && snapshot.content === current.content
}
