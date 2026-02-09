"use client";
import React, { useState } from 'react';
import ComponentCard from '../../common/ComponentCard';
import Label from '../Label';
import Input from '../input/InputField';
import TextArea from '../input/TextArea';

export default function BusinessInputs() {
  const [message, setMessage] = useState("");
  const options = [
    { value: "marketing", label: "Marketing" },
    { value: "template", label: "Template" },
    { value: "development", label: "Development" },
  ];
  return (
    <ComponentCard title="Business Inputs">
      <div className="space-y-6">
        <div>
          <Label>name</Label>
          <Input type="text" placeholder="Enter business name"/>
        </div>
        <div>
          <Label>Description</Label>
          <TextArea
            value={message}
            onChange={(value) => setMessage(value)}
            rows={6}
          />
        </div>
      </div>
    </ComponentCard>
  );
}
