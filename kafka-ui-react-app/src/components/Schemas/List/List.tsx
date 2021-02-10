import { SchemaSubject } from 'generated-sources';
import React from 'react';
import Breadcrumb from '../../common/Breadcrumb/Breadcrumb';
import ListItem from './ListItem';

interface ListProps {
  schemas: SchemaSubject[];
}

const List: React.FC<ListProps> = ({ schemas }) => {
  return (
    <div className="section">
      <Breadcrumb>Schema Registry</Breadcrumb>
      <div className="box">
        <table className="table is-striped is-fullwidth">
          <thead>
            <tr>
              <th>Schema Name</th>
            </tr>
          </thead>
          <tbody>
            {schemas.map(({ subject }) => (
              <ListItem subject={subject} />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default List;
