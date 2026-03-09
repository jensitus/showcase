const webpack = require('webpack');
const pkg = require('./package.json');
const path = require("path");
const { ModuleFederationPlugin } = require('webpack').container;
const { dependencies } = require('./package.json');

const aliases = {
    'styled-components': path.join(path.resolve(__dirname, '.'), "node_modules", "styled-components"),
    'react': path.join(path.resolve(__dirname, '.'), "node_modules", "react"),
    'react-dom': path.join(path.resolve(__dirname, '.'), "node_modules", "react-dom"),
    'react-router-dom': path.join(path.resolve(__dirname, '.'), "node_modules", "react-router-dom"),
    'i18next': path.join(path.resolve(__dirname, '.'), "node_modules", "i18next"),
    'react-i18next': path.join(path.resolve(__dirname, '.'), "node_modules", "react-i18next")
};

module.exports = (webpackConfig, options) => {

    const webpackVariable = {
        ...webpackConfig,
        resolve: {
            ...webpackConfig.resolve,
            alias: aliases
        },
        plugins: [
            ...webpackConfig.plugins,
            new ModuleFederationPlugin({
                name: "business-cockpit",
                shared: {
                    react: {
                        eager: true,
                        singleton: true,
                        requiredVersion: dependencies["react"],
                    },
                    "react-dom": {
                        eager: true,
                        singleton: true,
                        requiredVersion: dependencies["react-dom"],
                    },
                    "react-router-dom": {
                        eager: true,
                        singleton: true,
                        requiredVersion: dependencies["react-router-dom"],
                    },
                },
            }),
        ]
    }
    return webpackVariable;
};
