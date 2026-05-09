import { useCallback, useState } from 'react';
import type { ToastMessage } from '../components/ToastHost';

let toastSequence = 1;

export function useToast() {
  const [messages, setMessages] = useState<ToastMessage[]>([]);

  const notify = useCallback((type: ToastMessage['type'], text: string) => {
    const id = toastSequence++;
    setMessages((current) => [...current, { id, type, text }]);
    window.setTimeout(() => {
      setMessages((current) => current.filter((message) => message.id !== id));
    }, 3200);
  }, []);

  return { messages, notify };
}
