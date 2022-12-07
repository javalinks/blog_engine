package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "page", indexes = @Index(columnList = "path", unique = true))
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "site_id", nullable = false)
    private DbSite site;

    @Column(nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT NOT NULL")
    private String content;

    @Column(columnDefinition = "TEXT NOT NULL")
    private String path;

}
