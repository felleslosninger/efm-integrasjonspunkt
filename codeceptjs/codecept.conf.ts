import { setHeadlessWhen, setCommonPlugins } from '@codeceptjs/configure';
// turn on headless mode when running with HEADLESS=true environment variable
// export HEADLESS=true && npx codeceptjs run
setHeadlessWhen(process.env.HEADLESS);

// enable all common plugins https://github.com/codeceptjs/configure#setcommonplugins
setCommonPlugins();

export const config: CodeceptJS.MainConfig = {
  tests: './*_test.ts',
  output: './output',
  helpers: {
    REST: {
      endpoint: 'http://localhost:9093/api'
    },
    JSONResponse: {
    },
    Puppeteer: {
      url: 'http://localhost:9093/api',
      show: false,
      restart: true
    },
    Integrasjonspunkt: {
      require: './integrasjonspunkt_helper.js',
    },
  },
  include: {},
  name: 'codeceptjs'
}
