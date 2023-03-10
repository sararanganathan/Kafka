import React from 'react';
import { render } from '@testing-library/react';
import { GlobalSettingsContext } from 'components/contexts/GlobalSettingsContext';
import { useClusters } from 'lib/hooks/api/clusters';
import Dashboard from 'components/Dashboard/Dashboard';
import { Cluster, ServerStatus } from 'generated-sources';
import theme from 'theme/theme';
import { ThemeProvider } from 'styled-components';
import { BrowserRouter } from 'react-router-dom';

interface DataType {
  data: Cluster[];
  isSuccess: boolean;
}
jest.mock('lib/hooks/api/clusters');
const mockedUsedNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockedUsedNavigate,
}));
describe('Dashboard component', () => {
  const renderComponent = (hasDynamicConfig: boolean, data: DataType) => {
    const useClustersMock = useClusters as jest.Mock;
    useClustersMock.mockReturnValue(data);
    render(
      <GlobalSettingsContext.Provider value={{ hasDynamicConfig }}>
        <ThemeProvider theme={theme}>
          <BrowserRouter>
            <Dashboard />
          </BrowserRouter>
        </ThemeProvider>
      </GlobalSettingsContext.Provider>
    );
  };
  it('redirects to new cluster configuration page if there are no clusters and dynamic config is enabled', async () => {
    await renderComponent(true, { data: [], isSuccess: true });

    expect(mockedUsedNavigate).toHaveBeenCalled();
  });

  it('should not navigate to new cluster config page when there are clusters', async () => {
    await renderComponent(true, {
      data: [{ name: 'Cluster 1', status: ServerStatus.ONLINE }],
      isSuccess: true,
    });

    expect(mockedUsedNavigate).not.toHaveBeenCalled();
  });

  it('should not navigate to new cluster config page when there are no clusters and hasDynamicConfig is false', async () => {
    await renderComponent(false, {
      data: [],
      isSuccess: true,
    });

    expect(mockedUsedNavigate).not.toHaveBeenCalled();
  });
});
