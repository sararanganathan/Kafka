import React, { useEffect, useState } from 'react';
import * as S from 'components/Wizard/WizardForm/WizardForm.styled';
import { Button } from 'components/common/Button/Button';
import Input from 'components/common/Input/Input';
import { useFieldArray, useFormContext } from 'react-hook-form';
import PlusIcon from 'components/common/Icons/PlusIcon';
import IconButtonWrapper from 'components/common/Icons/IconButtonWrapper';
import CloseIcon from 'components/common/Icons/CloseIcon';
import Heading from 'components/common/heading/Heading.styled';
import Checkbox from 'components/common/Checkbox/Checkbox';

const KafkaConnect = () => {
  const [newKafkaConnect, setNewKafkaConnect] = useState(false);
  const { control, getValues, reset, watch } = useFormContext();
  const showConnects = (e: React.MouseEvent<HTMLButtonElement, MouseEvent>) => {
    e.preventDefault();
    setNewKafkaConnect(!newKafkaConnect);
  };
  const { fields, append, remove } = useFieldArray({
    control,
    name: 'kafkaConnect',
  });
  const connects = getValues('kafkaConnect');
  useEffect(() => {
    if (connects.length < 1) {
      setNewKafkaConnect(false);
      reset({
        ...getValues(),
        kafkaConnect: [],
      });
    }
  }, [connects]);

  return (
    <>
      <Heading level={3}>Kafka Connect</Heading>
      {newKafkaConnect ? (
        <S.ArrayFieldWrapper>
          {fields.map((item, index) => (
            <div key={item.id}>
              <S.ConnectInputWrapper>
                <div>
                  <Input
                    label="Kafka Connect name *"
                    name={`kafkaConnect.${index}.name`}
                    placeholder="Name"
                    type="text"
                    hint="Given name for the Kafka Connect cluster"
                    withError
                  />
                  <Input
                    label="Kafka Connect URL *"
                    name={`kafkaConnect.${index}.url`}
                    placeholder="URl"
                    type="text"
                    hint="Address of the Kafka Connect service endpoint"
                    withError
                  />
                  <Checkbox
                    name={`kafkaConnect.${index}.isAuth`}
                    label="Kafka Connect is secured with auth?"
                    cursor="pointer"
                  />
                </div>
                <S.RemoveButton onClick={() => remove(index)}>
                  <IconButtonWrapper aria-label="deleteProperty">
                    <CloseIcon aria-hidden />
                  </IconButtonWrapper>
                </S.RemoveButton>
              </S.ConnectInputWrapper>
              {watch(`kafkaConnect.${index}.isAuth`) && (
                <S.InputContainer>
                  <Input
                    label="Username"
                    name={`kafkaConnect.${index}.username`}
                    type="text"
                    withError
                  />
                  <Input
                    label="Password"
                    name={`kafkaConnect.${index}.password`}
                    type="password"
                    withError
                  />
                </S.InputContainer>
              )}
            </div>
          ))}
          <div>
            <Button
              type="button"
              buttonSize="M"
              buttonType="secondary"
              onClick={() =>
                append({
                  name: '',
                  url: '',
                  isAuth: false,
                  username: '',
                  password: ',',
                })
              }
            >
              <PlusIcon />
              Add Kafka Connect
            </Button>
          </div>
        </S.ArrayFieldWrapper>
      ) : (
        <div>
          <Button
            buttonSize="M"
            buttonType="primary"
            onClick={(e) => showConnects(e)}
          >
            Add Kafka Connect
          </Button>
        </div>
      )}
    </>
  );
};
export default KafkaConnect;
