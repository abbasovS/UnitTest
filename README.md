# 📈 Trade Microservice (trade-ms)

Bu layihə, yüksək performanslı ticarət əməliyyatlarını (trading operations) idarə edən bir mikroservisdir. İstifadəçilərə virtual balans ilə aktiv pozisiyalar açmağa, limit sifarişləri (pending orders) yerləşdirməyə və real-vaxt qiymətləri ilə bazarı izləməyə imkan verir.



## ✨ Özəlliklər
- **Trade Management:** LONG/SHORT pozisiyaların açılması və bağlanması.
- **Order Types:** Market və Pending (Limit) order dəstəyi.
- **Risk Management:** Avtomatik Likvidasiya qiyməti hesablama və TP/SL (Take Profit/Stop Loss) validasiyaları.
- **Real-time Data:** Feign Client vasitəsilə xarici qiymət servisləri ilə inteqrasiya.
- **Security:** Virtual balansın və dondurulmuş (frozen) balansın `Optimistic Locking` ilə təhlükəsiz idarə edilməsi.

## 🛠 Texnologiya Steki
- **Backend:** Java 21, Spring Boot 3.4.2
- **Database:** PostgreSQL (Verilənlərin saxlanması), Liquibase (Miqrasiya)
- **Communication:** Spring Cloud OpenFeign
- **Testing:** JUnit 5, Mockito (Unit & Slice Testing)
- **Documentation:** Swagger/OpenAPI

