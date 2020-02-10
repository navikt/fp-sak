package no.nav.foreldrepenger.behandlingslager.behandling.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.hibernate.jpa.QueryHints;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;
import no.nav.vedtak.util.FPDateUtil;

/**
 * Ulike spesialmetoder for å hente opp behandlinger som er kandidater for videre spesiell prosessering, slik som
 * etterkontroll gjenopptagelse av behandlinger på vent og lignende.
 * <p>
 * Disse vil bil brukt i en trigging av videre prosessering, behandling, kontroll, evt. henlegging eller avslutting.
 */

@ApplicationScoped
public class BehandlingKandidaterRepository {

    private static final Set<AksjonspunktDefinisjon> AUTOPUNKTER = List.of(AksjonspunktDefinisjon.values()).stream().filter(a -> AksjonspunktType.AUTOPUNKT.equals(a.getAksjonspunktType())).collect(Collectors.toSet());
    private static final Set<BehandlingStatus> AVSLUTTENDE_STATUS = BehandlingStatus.getFerdigbehandletStatuser();
    private static final String AVSLUTTENDE_KEY = "avsluttetOgIverksetterStatus";
    private EntityManager entityManager;

    BehandlingKandidaterRepository() {
        // for CDI proxy
    }

    @Inject
    public BehandlingKandidaterRepository(@VLPersistenceUnit EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    public List<Behandling> finnBehandlingerMedUtløptBehandlingsfrist() {
        return Stream.concat(finnUtløpteBehandlingerForEnkleTyper().stream(),
            finnUtløpteBehandlingerEndringssøknader().stream())
            .collect(Collectors.toList());
    }

    private List<Behandling> finnUtløpteBehandlingerEndringssøknader() {

        TypedQuery<Behandling> query = entityManager.createQuery(
            "SELECT behandling FROM Behandling behandling " +
                "INNER JOIN BehandlingÅrsak behandling_arsak " +
                "ON behandling=behandling_arsak.behandling " +
                "WHERE NOT behandling.status IN (:avsluttetOgIverksetterStatus) " +
                "AND behandling.behandlingstidFrist< :idag " +
                "AND behandling_arsak.behandlingÅrsakType = :endringType " +
                "AND behandling.behandlingType = :revurderingType", //$NON-NLS-1$
            Behandling.class);

        query.setParameter("idag", FPDateUtil.iDag()); //$NON-NLS-1$
        query.setParameter("revurderingType", BehandlingType.REVURDERING);
        query.setParameter("endringType", BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        query.setParameter(AVSLUTTENDE_KEY, AVSLUTTENDE_STATUS);
        query.setHint(QueryHints.HINT_READONLY, "true"); //$NON-NLS-1$
        return query.getResultList();
    }

    private List<Behandling> finnUtløpteBehandlingerForEnkleTyper() {
        Set<BehandlingType> behandlingTyperMedVarselBrev = hentBehandlingTyperMedBehandlingstidVarselBrev();

        TypedQuery<Behandling> query = entityManager.createQuery(
            "FROM Behandling behandling " +
                "WHERE NOT behandling.status IN (:avsluttetOgIverksetterStatus) " +
                "AND behandling.behandlingstidFrist< :idag " +
                "AND behandling.behandlingType in (:list)", //$NON-NLS-1$
            Behandling.class);

        query.setParameter("idag", FPDateUtil.iDag()); //$NON-NLS-1$
        query.setParameter("list", behandlingTyperMedVarselBrev);
        query.setParameter(AVSLUTTENDE_KEY, AVSLUTTENDE_STATUS);
        query.setHint(QueryHints.HINT_READONLY, "true"); //$NON-NLS-1$
        return query.getResultList();
    }

    private Set<BehandlingType> hentBehandlingTyperMedBehandlingstidVarselBrev() {
        return BehandlingType.kodeMap().values().stream()
            .filter(BehandlingType::isBehandlingstidVarselbrev)
            .collect(Collectors.toSet());
    }

    public List<Behandling> finnBehandlingerForAutomatiskGjenopptagelse() {

        Set<AksjonspunktDefinisjon> køetKode = Set.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING);

        Set<AksjonspunktDefinisjon> autopunktKoder = AUTOPUNKTER.stream().filter(a -> !køetKode.contains(a)).collect(Collectors.toSet());

        LocalDateTime naa = FPDateUtil.nå();

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            " SELECT DISTINCT b " +
                " FROM Aksjonspunkt ap " +
                " INNER JOIN ap.behandling b " +
                " WHERE ap.status IN :aapneAksjonspunktKoder " +
                "   AND ap.aksjonspunktDefinisjon IN :autopunktKoder " +
                "   AND ap.fristTid < :naa ",
            Behandling.class);
        query.setHint(QueryHints.HINT_READONLY, "true");
        query.setParameter("aapneAksjonspunktKoder", AksjonspunktStatus.getÅpneAksjonspunktStatuser());
        query.setParameter("autopunktKoder", autopunktKoder);
        query.setParameter("naa", naa);

        return query.getResultList();
    }

    public List<Behandling> finnRevurderingerPåVentIKompletthet() {

        TypedQuery<Behandling> query = entityManager.createQuery(
            "SELECT behandling FROM Behandling behandling " +
                "INNER JOIN Aksjonspunkt ap on ap.behandling.id=behandling.id " +
                " WHERE ap.status IN :aapneAksjonspunktKoder " +
                "   AND ap.aksjonspunktDefinisjon = :aksjonspunkt " +
                "   AND behandling.behandlingType = :behandlingType",
            Behandling.class);
        query.setHint(QueryHints.HINT_READONLY, "true");
        query.setParameter("aapneAksjonspunktKoder", AksjonspunktStatus.getÅpneAksjonspunktStatuser());
        query.setParameter("aksjonspunkt", AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD);
        query.setParameter("behandlingType", BehandlingType.REVURDERING);

        return query.getResultList();
    }

    public List<Behandling> finnBehandlingerIkkeAvsluttetPåAngittEnhet(String enhetId) {

        TypedQuery<Behandling> query = entityManager.createQuery(
            "FROM Behandling behandling " +
                "WHERE behandling.status NOT IN (:avsluttetOgIverksetterStatus) " +
                "  AND behandling.behandlendeEnhet = :enhet ", //$NON-NLS-1$
            Behandling.class);

        query.setParameter("enhet", enhetId); //$NON-NLS-1$
        query.setParameter(AVSLUTTENDE_KEY, AVSLUTTENDE_STATUS);
        query.setHint(QueryHints.HINT_READONLY, "true"); //$NON-NLS-1$
        return query.getResultList();
    }

    public List<Behandling> finnÅpneBehandlingerUtenÅpneAksjonspunktEllerAutopunkt() {

        TypedQuery<Behandling> query = entityManager.createQuery(
            "SELECT bh FROM Behandling bh " +
                "WHERE bh.status NOT IN (:avsluttetOgIverksetterStatus) " +
                "  AND NOT EXISTS (SELECT ap FROM Aksjonspunkt ap WHERE ap.behandling=bh AND ap.status = :status) ", //$NON-NLS-1$
            Behandling.class);

        query.setParameter(AVSLUTTENDE_KEY, AVSLUTTENDE_STATUS); //$NON-NLS-1$
        query.setParameter("status", AksjonspunktStatus.OPPRETTET); //$NON-NLS-1$
        query.setHint(QueryHints.HINT_READONLY, "true"); //$NON-NLS-1$
        return query.getResultList();
    }
}
