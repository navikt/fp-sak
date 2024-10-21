package no.nav.foreldrepenger.datavarehus.metrikker;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summarizingLong;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.hibernate.jpa.HibernateHints;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;

@ApplicationScoped
public class DokumentStatistikkRepository {
    private EntityManager entityManager;

    DokumentStatistikkRepository() {
        // for CDI proxy
    }

    @Inject
    public DokumentStatistikkRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<DokumentStatistikk> hentAntallDokumenttyper() {
        var query = entityManager.createQuery("""
            select md.dokumentTypeId, count(1) as antall
            from MottattDokument md
            group by md.dokumentTypeId
            """, DokumentTypeQR.class)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        var behandlingStatistikkEntitet = query.getResultList();
        return mapTilBehandlingStatistikk(behandlingStatistikkEntitet);
    }

    static List<DokumentStatistikk> mapTilBehandlingStatistikk(List<DokumentTypeQR> behandlingStatistikkEntitet) {
        return behandlingStatistikkEntitet.stream()
            .map(DokumentStatistikkRepository::tilBehandlingStatistikk)
            .collect(groupingBy(bs -> Optional.ofNullable(bs.dokumenttype()).orElse(Dokumenttype.ANNET),
                summarizingLong(bs -> Optional.ofNullable(bs.antall()).orElse(0L))))
            .entrySet()
            .stream()
            .map(dokumentTypeEntry -> new DokumentStatistikk(dokumentTypeEntry.getKey(), dokumentTypeEntry.getValue().getSum()))
            .toList();
    }

    private static DokumentStatistikk tilBehandlingStatistikk(DokumentTypeQR entitet) {
        return new DokumentStatistikk(tilDokumenttype(entitet.dokumentTypeId()), entitet.antall());
    }

    private static Dokumenttype tilDokumenttype(DokumentTypeId dokumentTypeId) {
        return switch (dokumentTypeId) {
            case SØKNAD_ENGANGSSTØNAD_FØDSEL, SØKNAD_ENGANGSSTØNAD_ADOPSJON -> Dokumenttype.ENGANGSSTØNAD;
            case SØKNAD_FORELDREPENGER_FØDSEL, SØKNAD_FORELDREPENGER_ADOPSJON -> Dokumenttype.FORELDREPENGER;
            case SØKNAD_SVANGERSKAPSPENGER, I000109 -> Dokumenttype.SVANGERSKAPSPENGER;
            case FORELDREPENGER_ENDRING_SØKNAD, FLEKSIBELT_UTTAK_FORELDREPENGER -> Dokumenttype.ENDRINGSSØKNAD;
            case INNTEKTSMELDING -> Dokumenttype.INNTEKTSMELDING;
            case KLAGE_DOKUMENT, KLAGE_ETTERSENDELSE -> Dokumenttype.KLAGE_ANKE;
            // case TILBAKEKREVING_UTTALSELSE, TILBAKEBETALING_UTTALSELSE -> Dokumenttype.TILBAKE; Mottas ikke i fpsak. Vil være 0
            case DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL, DOKUMENTASJON_AV_OMSORGSOVERTAKELSE, BEKREFTELSE_VENTET_FØDSELSDATO,
                FØDSELSATTEST, TERMINBEKREFTELSE -> Dokumenttype.FAMHENDELSE;
            case LEGEERKLÆRING, DOK_INNLEGGELSE, BESKRIVELSE_FUNKSJONSNEDSETTELSE, MOR_INNLAGT, MOR_SYK,
                FAR_SYK, FAR_INNLAGT, BARN_INNLAGT-> Dokumenttype.SYKDOM;
            case DOK_FERIE, DOK_ARBEIDSFORHOLD, BEKREFTELSE_FRA_ARBEIDSGIVER, BEKREFTELSE_FRA_STUDIESTED,
                BEKREFTELSE_DELTAR_KVALIFISERINGSPROGRAM, DOK_HV, DOK_NAV_TILTAK, MOR_ARBEID_STUDIE, MOR_ARBEID, MOR_STUDIE,
                MOR_KVALIFISERINGSPROGRAM, I000112-> Dokumenttype.AKTIVITETSKRAV;
            case null -> Dokumenttype.ANNET;
            default -> Dokumenttype.ANNET;
        };

    }

    record DokumentTypeQR(DokumentTypeId dokumentTypeId, Long antall) { }


    public record DokumentStatistikk(Dokumenttype dokumenttype, Long antall) { }

    public enum Dokumenttype {
        ENGANGSSTØNAD,
        FORELDREPENGER,
        SVANGERSKAPSPENGER,
        ENDRINGSSØKNAD,
        INNTEKTSMELDING,
        KLAGE_ANKE,
        FAMHENDELSE,
        SYKDOM,
        AKTIVITETSKRAV,
        ANNET
    }
}
