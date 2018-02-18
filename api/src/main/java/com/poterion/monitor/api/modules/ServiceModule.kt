package com.poterion.monitor.api.modules

import com.poterion.monitor.api.controllers.ServiceController
import com.poterion.monitor.data.services.ServiceConfig

interface ServiceModule<out Conf : ServiceConfig, out Controller : ServiceController<Conf>> :
        Module<Conf, Controller>