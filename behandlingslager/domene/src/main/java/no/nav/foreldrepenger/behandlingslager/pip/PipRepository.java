package no.nav.foreldrepenger.behandlingslager.pip;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
public class PipRepository {

    private static final LRUCache<UUID, Saksnummer> BEH_SAK = new LRUCache<>(20000, TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS));
    private static final LRUCache<String, AktørId> SAK_EIER = new LRUCache<>(5000, TimeUnit.MILLISECONDS.convert(2, TimeUnit.HOURS));
    private static final LRUCache<String, Set<AktørId>> SAK_AKTØR = new LRUCache<>(1000, TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES));

    private static final String SAKSNUMMER = "saksnummer";
    private static final String BUUID = "behandlingUuid";
    private EntityManager entityManager;

    public PipRepository() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
    }

    @Inject
    public PipRepository( EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<Saksnummer> hentSaksnummerForBehandlingUuid(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, BUUID);
        var cachedSak = BEH_SAK.get(behandlingUuid);
        if (cachedSak != null) {
            BEH_SAK.put(behandlingUuid, cachedSak);
            return Optional.of(cachedSak);
        }

        var query = entityManager.createQuery("SELECT f.saksnummer saksnummer FROM Behandling b JOIN Fagsak f ON b.fagsak = f WHERE b.uuid = :behandlingUuid")
            .setParameter(BUUID, behandlingUuid);
        @SuppressWarnings("unchecked")
        List<Saksnummer> resultater = query.getResultList();
        var sak = resultater.stream().findFirst();
        sak.ifPresent(s -> BEH_SAK.put(behandlingUuid, s));
        return sak;
    }

    public Optional<PipBehandlingsData> hentDataForBehandlingUuid(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, BUUID);

        var sql = """
            SELECT b.status, b.ansvarligSaksbehandler, b.id, f.fagsakStatus, f.saksnummer
            FROM Behandling b JOIN Fagsak f ON b.fagsak = f
            WHERE b.uuid = :behandlingUuid
            """;

        var query = entityManager.createQuery(sql)
            .setParameter(BUUID, behandlingUuid);
        @SuppressWarnings("unchecked")
        List<Object[]> resultater = query.getResultList();
        return resultater.stream().findFirst().map(r -> mapPipBehandlingsdata(behandlingUuid, r));
    }

    private static PipBehandlingsData mapPipBehandlingsdata(UUID behandlingUuid, Object[] resultat) {
        return new PipBehandlingsData((BehandlingStatus) resultat[0], (String) resultat[1], (Long) resultat[2], behandlingUuid, (FagsakStatus) resultat[3], (Saksnummer) resultat[4]);
    }

    public Optional<AktørId> hentAktørIdSomEierFagsak(Saksnummer saksnummer) {
        Objects.requireNonNull(saksnummer, SAKSNUMMER);
        if (SAK_EIER.get(saksnummer.getVerdi()) != null) {
            return Optional.of(SAK_EIER.get(saksnummer.getVerdi()));
        }
        var sql = """
            SELECT br.aktørId FROM Fagsak fag
            JOIN Bruker br ON fag.navBruker = br
            WHERE fag.saksnummer = :saksnummer  AND br.aktørId IS NOT NULL
            """;

        var query = entityManager.createQuery(sql)
            .setParameter(SAKSNUMMER, saksnummer);

        @SuppressWarnings("unchecked")
        List<AktørId> aktørIdList = query.getResultList();
        var eier = aktørIdList.stream().findFirst();
        eier.ifPresent(e -> SAK_EIER.put(saksnummer.getVerdi(), e));
        return eier;
    }

    public Set<AktørId> hentAktørIdKnyttetTilSaksnummer(String saksnummer) {
        Objects.requireNonNull(saksnummer, SAKSNUMMER);

        if (SAK_AKTØR.get(saksnummer) != null) {
            return SAK_AKTØR.get(saksnummer);
        }

        var sql = """
            SELECT por.AKTOER_ID From Fagsak fag
            JOIN BEHANDLING beh ON fag.ID = beh.FAGSAK_ID
            JOIN GR_PERSONOPPLYSNING grp ON grp.behandling_id = beh.ID
            JOIN PO_INFORMASJON poi ON grp.registrert_informasjon_id = poi.ID
            JOIN PO_PERSONOPPLYSNING por ON poi.ID = por.po_informasjon_id
            WHERE fag.SAKSNUMMER = (:saksnummer) AND grp.aktiv = 'J'
             UNION ALL
            SELECT br.AKTOER_ID FROM Fagsak fag
            JOIN Bruker br ON fag.BRUKER_ID = br.ID
            WHERE fag.SAKSNUMMER = (:saksnummer) AND br.AKTOER_ID IS NOT NULL
             UNION ALL
            SELECT sa.AKTOER_ID From Fagsak fag
            JOIN BEHANDLING beh ON fag.ID = beh.FAGSAK_ID
            JOIN GR_PERSONOPPLYSNING grp ON grp.behandling_id = beh.ID
            JOIN SO_ANNEN_PART sa ON grp.so_annen_part_id = sa.ID
            WHERE fag.SAKSNUMMER = (:saksnummer) AND grp.aktiv = 'J' AND sa.AKTOER_ID IS NOT NULL
            """;

        var query = entityManager.createNativeQuery(sql)
            .setParameter(SAKSNUMMER, saksnummer);

        @SuppressWarnings("unchecked")
        List<String> aktørIdList = query.getResultList();
        var aktører = aktørIdList.stream().map(AktørId::new).collect(Collectors.toCollection(LinkedHashSet::new));
        SAK_AKTØR.put(saksnummer, aktører);
        return aktører;
    }

    @SuppressWarnings({ "unchecked", "cast" })
    public Set<Saksnummer> saksnummerForJournalpostId(Collection<String> journalpostId) {
        if (journalpostId.isEmpty()) {
            return Collections.emptySet();
        }
        var sql = "SELECT f.saksnummer FROM Journalpost j JOIN Fagsak f on j.fagsak = f WHERE j.journalpostId in (:journalpostId)";
        var query = entityManager.createQuery(sql)
            .setParameter("journalpostId", journalpostId.stream().map(JournalpostId::new).toList());
        List<Saksnummer> saksnummerList = query.getResultList();
        return new LinkedHashSet<>(saksnummerList);
    }

    public static boolean harAksjonspunktTypeOverstyring(Collection<AksjonspunktDefinisjon> aksjonspunktKoder) {
        return aksjonspunktKoder.stream()
            .map(AksjonspunktDefinisjon::getAksjonspunktType)
            .filter(Objects::nonNull)
            .anyMatch(AksjonspunktType.OVERSTYRING::equals);
    }
}
