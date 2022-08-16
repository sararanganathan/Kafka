import React from 'react';
import { render, WithRoute } from 'lib/testHelpers';
import { clusterConnectConnectorTasksPath } from 'lib/paths';
import Tasks from 'components/Connect/Details/Tasks/Tasks';
import { tasks } from 'lib/fixtures/kafkaConnect';
import { screen, within, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  useConnectorTasks,
  useRestartConnectorTask,
} from 'lib/hooks/api/kafkaConnect';
import { Task } from 'generated-sources';

jest.mock('lib/hooks/api/kafkaConnect', () => ({
  useConnectorTasks: jest.fn(),
  useRestartConnectorTask: jest.fn(),
}));

const path = clusterConnectConnectorTasksPath('local', 'ghp', '1');

const restartConnectorMock = jest.fn();

describe('Tasks', () => {
  beforeEach(() => {
    (useRestartConnectorTask as jest.Mock).mockImplementation(() => ({
      mutateAsync: restartConnectorMock,
    }));
  });

  const renderComponent = (currentData: Task[] | undefined = undefined) => {
    (useConnectorTasks as jest.Mock).mockImplementation(() => ({
      data: currentData,
    }));

    render(
      <WithRoute path={clusterConnectConnectorTasksPath()}>
        <Tasks />
      </WithRoute>,
      { initialEntries: [path] }
    );
  };

  it('renders empty table', () => {
    renderComponent();
    expect(screen.getByRole('table')).toBeInTheDocument();
    expect(screen.getByText('No tasks found')).toBeInTheDocument();
  });

  it('renders tasks table', () => {
    renderComponent(tasks);
    expect(screen.getAllByRole('row').length).toEqual(tasks.length + 1);

    expect(
      screen.getByRole('row', {
        name: '1 kafka-connect0:8083 RUNNING',
      })
    ).toBeInTheDocument();
  });

  it('renders truncates long trace and expands', () => {
    renderComponent(tasks);

    const trace = tasks[2]?.status?.trace || '';
    const truncatedTrace = trace.toString().substring(0, 100 - 3);

    const thirdRow = screen.getByRole('row', {
      name: `3 kafka-connect0:8083 RUNNING ${truncatedTrace}...`,
    });
    expect(thirdRow).toBeInTheDocument();

    const expandedDetails = screen.queryByText(trace);
    //  Full trace is not visible
    expect(expandedDetails).not.toBeInTheDocument();

    userEvent.click(thirdRow);

    expect(
      screen.getByRole('row', {
        name: trace,
      })
    ).toBeInTheDocument();
  });

  describe('Action button', () => {
    const expectDropdownExists = () => {
      const firstTaskRow = screen.getByRole('row', {
        name: '1 kafka-connect0:8083 RUNNING',
      });
      expect(firstTaskRow).toBeInTheDocument();
      const extBtn = within(firstTaskRow).getByRole('button', {
        name: 'Dropdown Toggle',
      });
      expect(extBtn).toBeEnabled();
      userEvent.click(extBtn);
      expect(screen.getByRole('menu')).toBeInTheDocument();
    };

    it('renders action button', () => {
      renderComponent(tasks);
      expectDropdownExists();
      expect(
        screen.getAllByRole('button', { name: 'Dropdown Toggle' }).length
      ).toEqual(tasks.length);
      // Action buttons are enabled
      const actionBtn = screen.getAllByRole('menuitem');
      expect(actionBtn[0]).toHaveTextContent('Restart task');
    });

    it('works as expected', async () => {
      renderComponent(tasks);
      expectDropdownExists();
      const actionBtn = screen.getAllByRole('menuitem');
      expect(actionBtn[0]).toHaveTextContent('Restart task');

      userEvent.click(actionBtn[0]);
      await waitFor(() => expect(restartConnectorMock).toHaveBeenCalled());
    });
  });
});
