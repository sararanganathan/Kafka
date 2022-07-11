import React, { PropsWithChildren, ReactElement } from 'react';
import {
  MemoryRouter,
  MemoryRouterProps,
  Route,
  Routes,
} from 'react-router-dom';
import { Provider } from 'react-redux';
import { ThemeProvider } from 'styled-components';
import theme from 'theme/theme';
import { render, RenderOptions, screen } from '@testing-library/react';
import { AnyAction, Store } from 'redux';
import { RootState } from 'redux/interfaces';
import { configureStore } from '@reduxjs/toolkit';
import rootReducer from 'redux/reducers';
import mockStoreCreator from 'redux/store/configureStore/mockStoreCreator';
import { QueryClient, QueryClientProvider } from 'react-query';

interface CustomRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  preloadedState?: Partial<RootState>;
  store?: Store<Partial<RootState>, AnyAction>;
  initialEntries?: MemoryRouterProps['initialEntries'];
}

interface WithRouterProps {
  children: React.ReactNode;
  path: string;
}

export const WithRoute: React.FC<WithRouterProps> = ({ children, path }) => {
  return (
    <Routes>
      <Route path={path} element={children} />
    </Routes>
  );
};

const customRender = (
  ui: ReactElement,
  {
    preloadedState,
    store = configureStore<RootState>({
      reducer: rootReducer,
      preloadedState,
    }),
    initialEntries,
    ...renderOptions
  }: CustomRenderOptions = {}
) => {
  // use new QueryClient instance for each test run to avoid issues with cache
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  // overrides @testing-library/react render.
  const AllTheProviders: React.FC<PropsWithChildren<unknown>> = ({
    children,
  }) => {
    return (
      <ThemeProvider theme={theme}>
        <Provider store={store}>
          <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={initialEntries}>
              {children}
            </MemoryRouter>
          </QueryClientProvider>
        </Provider>
      </ThemeProvider>
    );
  };
  return render(ui, { wrapper: AllTheProviders, ...renderOptions });
};

export { customRender as render };

export class EventSourceMock {
  url: string;

  close: () => void;

  open: () => void;

  error: () => void;

  onmessage: () => void;

  constructor(url: string) {
    this.url = url;
    this.open = jest.fn();
    this.error = jest.fn();
    this.onmessage = jest.fn();
    this.close = jest.fn();
  }
}

export const getTypeAndPayload = (store: typeof mockStoreCreator) => {
  return store.getActions().map(({ type, payload }) => ({ type, payload }));
};

export const getAlertActions = (mockStore: typeof mockStoreCreator) =>
  getTypeAndPayload(mockStore).filter((currentAction: AnyAction) =>
    currentAction.type.startsWith('alerts')
  );
