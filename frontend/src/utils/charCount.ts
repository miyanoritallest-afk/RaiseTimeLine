export const POST_MAX_LENGTH = 280

export function getCharCountClass(length: number): string {
  if (length >= 270) return 'char-count danger'
  if (length >= 260) return 'char-count warning'
  return 'char-count'
}
