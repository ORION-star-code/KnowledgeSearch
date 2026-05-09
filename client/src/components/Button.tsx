import type { ButtonHTMLAttributes, ReactNode } from 'react';

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  icon?: ReactNode;
};

export function Button({ variant = 'secondary', icon, children, className = '', ...props }: ButtonProps) {
  return (
    <button className={`btn btn-${variant} ${className}`} type="button" {...props}>
      {icon ? <span className="btn-icon">{icon}</span> : null}
      {children ? <span>{children}</span> : null}
    </button>
  );
}
