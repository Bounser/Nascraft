Welcome to the Nascraft configuration guide!

Made By Earth1283

This configuration guide will help you navigate `config.yml`
It is recommended to render this markdown on some platform such as github.
Let's begin with some general things to keep in mind:

# General things to keep in mind
Primarily, the plugin's shop is dynamically priced and roughly based on supply and demand. 
The more your players buy an item, the lower the prices will be. Note that this a ***very rough explanation*** of what is happening.
For a more detailed (and accurate) explaination of what is going on with the market, see the image below or visit the SpigotMC page of the plugin

Warning, light mode ahead!!!

![spigmc image](https://proxy.spigotmc.org/c767b2ec255ce011a48d0ed20fff0aad9c65cbfb/68747470733a2f2f692e696d6775722e636f6d2f425a59643876622e706e67)

For the rest of the config explanation, we would refer to config options in the following manner:

Let's get ourselves a config.yml first
```yaml
config1:
  config2:
    config3:
      config4: false
```
Here, we will refer to config2 as `config1.config2`, and config3 as `config1.config2.config3` and so on...

# Configuration
## Language & Storage (Databases)
`language` - This option allows you to change the locale of the plugin. If your server speaks Chinese (Simplified), you can use the locale `zh_CN`.
Here I will list all the exsisting locales: `en_US` | `es_ES` | `it_IT` | `de_DE` | `pt_BR` | `zh_CN`
They are the following listed in natural language: `english United States` | `Spanish` | `Italian` | `German` | `chinese Simplified`

`databse.type` - Here you can configure the type of your database. I will guide you through your configuration options.
- SQLite | Use a local file-based SQL storage system. This requires no extra setup
- MYSQL | Uses an external (or localhost, depends on your preferences) database for synchronization
- Redis | Uses a redis cache for plugin data transfers

I will not guide you through setting up MYSQL and/or Redis. That is not my job.

## Folia Support & Currency Controls
`folia` - Configuration options for the multithreaded server softare. You may view their source code repo [here](https://github.com/PaperMC/Folia). The options are pretty self-explanatory and I have provided the needed explanation within the config file.

`currencies` - Here you can modify the currency configuraitons for the economy

`currencies.default-currency` - The default currency provider. By default, it's vault. You can [download](https://www.spigotmc.org/resources/vault.34315/) it via its SpigotMC page.

`price-options` - Configure the procing in the plugin.

`price-options.noise` - the `price-options.noise.enabled` option toggles this feature. The other options (`price-options.noise.default-intensity` and `price-options.noise.noise-intensity` add on to this plugin)

that's all the work earth will do for this update. more will come via a new pull request
