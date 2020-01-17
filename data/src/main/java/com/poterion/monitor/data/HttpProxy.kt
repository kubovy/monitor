package com.poterion.monitor.data

import com.poterion.monitor.data.auth.BasicAuthConfig

class HttpProxy(var address: String? = null, var port: Int? = null, var auth: BasicAuthConfig? = null)