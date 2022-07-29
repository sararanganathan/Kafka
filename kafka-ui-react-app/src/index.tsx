import React from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import App from 'components/App';
import { store } from 'redux/store';
import { BASE_PATH } from 'lib/constants';
import 'theme/index.scss';

const container =
  document.getElementById('root') || document.createElement('div');
const root = createRoot(container);
root.render(
  <Provider store={store}>
    <BrowserRouter basename={BASE_PATH ?? '/'}>
      <App />
    </BrowserRouter>
  </Provider>
);
