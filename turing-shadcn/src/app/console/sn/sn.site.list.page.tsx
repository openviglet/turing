export default function SNSiteListPage() {
  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold text-foreground">Semantic Navigation Sites</h2>
        <button className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90">
          New Site
        </button>
      </div>
      
      <div className="bg-card rounded-lg shadow p-6">
        <p className="text-muted-foreground">
          List of semantic navigation sites will appear here.
        </p>
      </div>
    </div>
  );
}
