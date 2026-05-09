import { CheckCircle2, Info, XCircle } from 'lucide-react';

export type ToastMessage = {
  id: number;
  type: 'success' | 'error' | 'info';
  text: string;
};

type ToastHostProps = {
  messages: ToastMessage[];
};

export function ToastHost({ messages }: ToastHostProps) {
  return (
    <div className="toast-host" aria-live="polite">
      {messages.map((message) => (
        <div className={`toast toast-${message.type}`} key={message.id}>
          {message.type === 'success' ? <CheckCircle2 size={18} /> : null}
          {message.type === 'error' ? <XCircle size={18} /> : null}
          {message.type === 'info' ? <Info size={18} /> : null}
          <span>{message.text}</span>
        </div>
      ))}
    </div>
  );
}
