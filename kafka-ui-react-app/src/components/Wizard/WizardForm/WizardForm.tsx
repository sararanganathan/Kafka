import React from 'react';
import { Button } from 'components/common/Button/Button';
import { useForm, FormProvider } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import formSchema from 'components/Wizard/schema';
import { StyledForm } from 'components/common/Form/Form.styled';

import * as S from './WizardForm.styled';
import KafkaCluster from './KafkaCluster/KafkaCluster';
import Authentication from './Authentication/Authentication';
import SchemaRegistry from './SchemaRegistry/SchemaRegistry';
import Metrics from './Metrics/Metrics';

type SecurityProtocol = 'SASL_SSL' | 'SASL_PLAINTEXT';

type BootstrapServer = {
  host: string;
  port: string;
};
type SchemaRegistryType = {
  url: string;
  isAuth: boolean;
  username: string;
  password: string;
};
export type FormValues = {
  name: string;
  readOnly: boolean;
  bootstrapServers: BootstrapServer[];
  useTruststore: boolean;
  truststore?: {
    location: string;
    password: string;
  };

  securityProtocol?: SecurityProtocol;
  authentication: {
    method: 'none' | 'sasl';
  };
  schemaRegistry?: SchemaRegistryType;
  properties?: Record<string, string>;
};

interface WizardFormProps {
  initaialValues?: FormValues;
}

const Wizard: React.FC<WizardFormProps> = () => {
  const methods = useForm<FormValues>({
    mode: 'all',
    resolver: yupResolver(formSchema),
    defaultValues: {
      name: 'My test cluster',
      readOnly: true,
      bootstrapServers: [
        { host: 'loc1', port: '3001' },
        { host: 'loc', port: '3002' },
      ],
      useTruststore: false,
      securityProtocol: 'SASL_PLAINTEXT',
      authentication: {
        method: 'none',
      },
    },
  });

  const onSubmit = (data: FormValues) => {
    // eslint-disable-next-line no-console
    console.log('SubmitData', data);
    return data;
  };

  return (
    <FormProvider {...methods}>
      <StyledForm onSubmit={methods.handleSubmit(onSubmit)}>
        <KafkaCluster />
        <hr />
        <Authentication />
        <hr />
        <SchemaRegistry />
        <hr />
        <S.Section>
          <S.SectionName>Kafka Connect</S.SectionName>
          <div>
            <Button buttonSize="M" buttonType="primary">
              Add Kafka Connect
            </Button>
          </div>
        </S.Section>
        <Metrics />
        <hr />
        <S.ButtonWrapper>
          <Button buttonSize="L" buttonType="primary">
            Cancel
          </Button>
          <Button type="submit" buttonSize="L" buttonType="primary">
            Save
          </Button>
        </S.ButtonWrapper>
      </StyledForm>
    </FormProvider>
  );
};

export default Wizard;
