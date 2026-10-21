# Library & Bulk Updates FAQ

Learn how library refreshes work and why safety warnings are displayed during bulk operations.

---

## ⚠️ Why am I warned about large bulk updates and downloads?

When you initiate a global library update or queue hundreds of chapter downloads at once, ShinKu displays a cautionary notice.

### The Risks of Massive Bulk Requests:
1. **Source IP Bans:** Third-party web hosts monitor incoming traffic. If a single IP address sends 500 requests in under 60 seconds, their automated firewall may temporarily ban your IP address (HTTP 403 or 429) for hours or days.
2. **Server Overload:** Many community scanlation sites and indie hosts operate on limited hosting budgets. Aggressive parallel scraping risks degrading performance for everyone.
3. **Battery & Data Drain:** Fetching metadata, chapter lists, and cover images across hundreds of titles simultaneously consumes significant battery and cellular data.

---

## 💡 Best Practices for Smooth Updates

* **Use Categories:** Group your manga into categories (e.g. *Ongoing Weekly*, *Monthly*, *Completed*). In **More → Settings → Library → Global update**, select only your active categories to be updated automatically.
* **Exclude Completed Titles:** Mark finished manga as *Completed* so ShinKu does not waste requests checking them for new chapters.
* **Stagger Update Frequencies:** Set automatic background updates to run once every 12 to 24 hours while on Wi-Fi and charging.
