import React from 'react';
import { useParams } from 'react-router-dom';
import { Task, TaskId } from 'generated-sources';
import { ClusterName, ConnectName, ConnectorName } from 'redux/interfaces';
import Dropdown from 'components/common/Dropdown/Dropdown';
import DropdownItem from 'components/common/Dropdown/DropdownItem';
import VerticalElipsisIcon from 'components/common/Icons/VerticalElipsisIcon';
import * as C from 'components/common/Tag/Tag.styled';
import getTagColor from 'components/common/Tag/getTagColor';

interface RouterParams {
  clusterName: ClusterName;
  connectName: ConnectName;
  connectorName: ConnectorName;
}

export interface ListItemProps {
  task: Task;
  restartTask(payload: {
    clusterName: ClusterName;
    connectName: ConnectName;
    connectorName: ConnectorName;
    taskId: TaskId['task'];
  }): Promise<unknown>;
}

const ListItem: React.FC<ListItemProps> = ({ task, restartTask }) => {
  const { clusterName, connectName, connectorName } = useParams<RouterParams>();

  const restartTaskHandler = async () => {
    await restartTask({
      clusterName,
      connectName,
      connectorName,
      taskId: task.id?.task,
    });
  };

  return (
    <tr>
      <td>{task.status?.id}</td>
      <td>{task.status?.workerId}</td>
      <td>
        <C.Tag color={getTagColor(task.status)}>{task.status.state}</C.Tag>
      </td>
      <td>{task.status.trace || 'null'}</td>
      <td style={{ width: '5%' }}>
        <div>
          <Dropdown label={<VerticalElipsisIcon />} right>
            <DropdownItem onClick={restartTaskHandler} danger>
              <span>Restart task</span>
            </DropdownItem>
          </Dropdown>
        </div>
      </td>
    </tr>
  );
};

export default ListItem;
