# Bench---Spark-pipeline

<h3>Objective</h3>

<h3>Envirorment Installation</h3>

<h4>1 - WSL2 Activation and Configuration</h4>

It's necesary to install and activate Windows Subsystem for Linux 2, in order to use it with docker, you can follow this guide to install, configure and update WSL:

https://docs.microsoft.com/en-us/windows/wsl/install-win10

<h4>2 - Install Docker Desktop on Windows</h4>

Follow these instructions to download and install Docker on Windows:

https://docs.docker.com/docker-for-windows/install/

<h4>3 - Docker WSL Configuration</h4>

Once you have WSL2 configurated and Docker installed, you would be able to use the Docker Desktop on Windows integration with WSL using the Linux distribution installed for it, follow these instructions to configure the integration:

https://docs.docker.com/docker-for-windows/wsl/

<h4>4 - Project Execution</h4>

You must download the project file and unzip it, after that you have to make a copy in your WSL, and navigate to the root where is contained the main docker file and execute the next instruction in the Linux command console:

`docker-compose up`
