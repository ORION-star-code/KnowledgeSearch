import type { ToastMessage } from '../components/ToastHost';

export type Notify = (type: ToastMessage['type'], text: string) => void;

export type PageProps = {
  notify: Notify;
  navigate?: (view: string) => void;
};
