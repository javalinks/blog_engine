package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.DbSite;

import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<DbSite, Integer> {

    @Query(value = "SELECT * FROM site WHERE url LIKE :url", nativeQuery = true)
    DbSite getDbSiteByUrl(String url);

    @Query(value = "SELECT status FROM site", nativeQuery = true)
    List<String> indexingStatus();

    @Query(value = "SELECT * FROM site WHERE status LIKE :status", nativeQuery = true)
    List<DbSite> indexingSite(String status);
}
