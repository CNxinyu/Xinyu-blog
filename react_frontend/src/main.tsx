import { createRoot } from 'react-dom/client'
import Router from './routes';


import { Provider } from 'react-redux';
import { store } from './store';

createRoot(document.getElementById('root')!).render(
  <Provider store={store}>
    <Router />
  </Provider>
)
