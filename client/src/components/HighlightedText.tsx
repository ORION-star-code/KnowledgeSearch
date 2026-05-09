type HighlightedTextProps = {
  text?: string;
  fallback?: string;
};

export function HighlightedText({ text, fallback = '-' }: HighlightedTextProps) {
  const value = text || fallback;
  if (!value.includes('<em>')) {
    return <>{value}</>;
  }

  const parts = value.split(/(<em>|<\/em>)/g);
  let highlighted = false;
  return (
    <>
      {parts.map((part, index) => {
        if (part === '<em>') {
          highlighted = true;
          return null;
        }
        if (part === '</em>') {
          highlighted = false;
          return null;
        }
        return highlighted ? <mark key={`${part}-${index}`}>{part}</mark> : <span key={`${part}-${index}`}>{part}</span>;
      })}
    </>
  );
}
