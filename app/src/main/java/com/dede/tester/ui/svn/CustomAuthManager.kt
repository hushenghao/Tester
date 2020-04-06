package com.dede.tester.ui.svn

import org.tmatesoft.svn.core.auth.SVNAuthentication
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication
import org.tmatesoft.svn.core.auth.SVNSSHAuthentication
import org.tmatesoft.svn.core.auth.SVNUserNameAuthentication
import org.tmatesoft.svn.core.io.SVNRepository

/**
 * 自定义超时时间
 */
class CustomAuthManager : org.tmatesoft.svn.core.auth.BasicAuthenticationManager {

    constructor(userName: String?, password: String?) : this(
        arrayOf<SVNAuthentication>(
            SVNPasswordAuthentication.newInstance(
                userName,
                password?.toCharArray(),
                false,
                null,
                false
            ),
            SVNSSHAuthentication.newInstance(
                userName,
                password?.toCharArray(),
                -1,
                false,
                null,
                false
            ),
            SVNUserNameAuthentication.newInstance(userName, false, null, false)
        )
    )

    constructor(authentications: Array<SVNAuthentication>) : super(authentications)

    override fun getConnectTimeout(repository: SVNRepository?): Int {
        return 10 * 1000
    }

    override fun getReadTimeout(repository: SVNRepository?): Int {
        return 15 * 1000
    }
}