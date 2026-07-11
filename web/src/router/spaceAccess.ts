export interface SpaceAccessSession {
  loggedIn: boolean
  accountType?: string
  canManageProjects?: boolean
}

export function canEnterSpace(session: SpaceAccessSession): boolean {
  return session.loggedIn && session.accountType === 'SITE_USER'
}

export function canUseSpaceProjectManagement(session: SpaceAccessSession): boolean {
  return canEnterSpace(session)
}
