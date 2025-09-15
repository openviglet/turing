export class TurSNURL {
  private url: URL;

  constructor(urlString: string) {
    this.url = new URL(urlString);
  }

  getQuery(): string | null {
    return this.url.searchParams.get("q");
  }

  setQuery(value: string): void {
    this.url.searchParams.set("q", value);
    this.url.search = this.url.searchParams.toString();
  }

  toString(): string {
    return this.url.toString();
  }
}
