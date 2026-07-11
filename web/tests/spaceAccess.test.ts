import assert from 'node:assert/strict'
import { canEnterSpace, canUseSpaceProjectManagement } from '../src/router/spaceAccess.ts'

assert.equal(
  canEnterSpace({ loggedIn: true, accountType: 'SITE_USER', canManageProjects: false }),
  true,
  'logged-in users without project permission can enter the account center',
)

assert.equal(
  canEnterSpace({ loggedIn: false, accountType: 'SITE_USER', canManageProjects: true }),
  false,
  'anonymous users cannot enter the account center',
)

assert.equal(
  canEnterSpace({ loggedIn: true, accountType: 'ADMIN', canManageProjects: true }),
  false,
  'admin accounts from the back office cannot enter the site user account center',
)

assert.equal(
  canUseSpaceProjectManagement({ loggedIn: true, accountType: 'SITE_USER', canManageProjects: true }),
  true,
  'logged-in site users can use personal project management',
)

assert.equal(
  canUseSpaceProjectManagement({ loggedIn: true, accountType: 'SITE_USER', canManageProjects: false }),
  true,
  'personal project management does not depend on backend project permission',
)

assert.equal(
  canUseSpaceProjectManagement({ loggedIn: true, accountType: 'ADMIN', canManageProjects: true }),
  false,
  'admin accounts cannot use site user project management',
)
