package com.poterion.monitor.api.modules

import com.poterion.monitor.api.controllers.NotifierController
import com.poterion.monitor.data.notifiers.NotifierConfig

interface NotifierModule<out Conf : NotifierConfig, out Controller : NotifierController<Conf>> :
		Module<Conf, Controller>