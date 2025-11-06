export default function TokenInstanceListPage() {
  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold text-foreground">Token Instances</h2>
        <button className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90">
          New Instance
        </button>
      </div>
      
      <div className="bg-card rounded-lg shadow p-6">
        <p className="text-muted-foreground">
          List of token instances will appear here.
        </p>
      </div>
    </div>
  );
}
