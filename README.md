# Monitor Application

The core of the application is made up from ``Data``, ``API``, ``Control`` and ``UI`` modules.
The ``Data`` module simply contains the different POJOs, e.g., ``Config``, ``Status``, 
``Priority``. The ``API`` module contains interface logic, e.g., ``Module``, ``ServiceModule``,
``NotifierModule``. The ``Control`` module puts the things together and the ``UI`` module
contains some UI specific stuff for modules which want to use UI.

![Modules](/assembly/src/uml/Main.png)

Then we have a bunch of modules: ``Jenkins``, ``Sonar``, ``Raspi W2812`` and ``System Tray``.
The former two are ``Service`` modules and the later two are ``Notifier`` modules. ``Service``
modules check a status of a certain service and lets the ``Controller`` know through the
``API`` module. While ``Notifier`` modules are notified by the ``Controller`` over the ``API``
about ``Status`` changes and react to them.

What the two concrete services do is pretty much self-explanatory. The ``Jenkins`` module
checks the status of one or more Jenkins jobs and the ``Sonar`` module check the status of 
one or more sonar projects. Also each module can be instantiated multiple times so multiple 
Jenkins or Sonar installations may be included.

The ``System Tray`` ``Notifier`` displays the status in the system tray. It also uses the 
``UI`` module which enables to access the configuration window through the system tray.

The ``Raspi W2812`` module connects to [Raspi W2812 light](https://blog.kubovy.eu/2018/02/11/status-light-with-raspberry-pi-zero-and-w2812-led-strip/) 
via USB and changes the light pattern, color, etc.

![StatusItem](/data/src/uml/StatusItem.png)

Everything revolves around a ``StatusItem`` in the ``Data`` module. ``Service`` modules have 
to translate the state of their services to it and send a list of such entities to the 
``Controller`` who stores them in a list (one ``StatusItem`` per item). An item is, e.g., a 
Jenkins Job or a Sonar Project. The ``Priority`` is taken from the configuration where you 
can set a default ``Priority`` for a whole ``Service`` and per each service item.

Every time the collection of ``StatusItems`` changes the ``Notifier`` modules get notified. 
Depending on their capability they can display them. So for example the ``System Tray`` module
can display only one ``StatusItem`` so it chooses the one with the worst ``Status``. Our 
``Raspi W2812`` module chooses a collection of ``StatusItems`` with the worst ``Status`` and 
highest ``Priority``. Then it rotates the corresponding light patterns and colors.

The last ``Assembly`` module just takes the modules and build a Fat Jar with a main method so 
we can start it. The ``Assembly`` module and the concept is done in a way so we can replace 
the ``Assembly`` with, e.g., ``Web`` module and start the whole thing as a web service.


