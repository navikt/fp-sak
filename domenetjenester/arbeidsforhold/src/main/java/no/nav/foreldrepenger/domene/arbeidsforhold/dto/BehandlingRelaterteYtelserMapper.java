package no.nav.foreldrepenger.domene.arbeidsforhold.dto;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.vedtak.konfig.Tid;

public class BehandlingRelaterteYtelserMapper {

    private static final Map<FagsakYtelseType, RelatertYtelseType> YTELSE_TYPE_MAP = Map.of(FagsakYtelseType.ENGANGSTØNAD,
        RelatertYtelseType.ENGANGSTØNAD, FagsakYtelseType.FORELDREPENGER, RelatertYtelseType.FORELDREPENGER, FagsakYtelseType.SVANGERSKAPSPENGER,
        RelatertYtelseType.SVANGERSKAPSPENGER);

    public static final List<RelatertYtelseType> RELATERT_YTELSE_TYPER_FOR_SØKER = List.of(RelatertYtelseType.FORELDREPENGER,
        RelatertYtelseType.ENGANGSTØNAD, RelatertYtelseType.SYKEPENGER, RelatertYtelseType.DAGPENGER, RelatertYtelseType.ARBEIDSAVKLARINGSPENGER,
        RelatertYtelseType.SVANGERSKAPSPENGER, RelatertYtelseType.OMSORGSPENGER, RelatertYtelseType.OPPLÆRINGSPENGER,
        RelatertYtelseType.PLEIEPENGER_SYKT_BARN, RelatertYtelseType.PLEIEPENGER_NÆRSTÅENDE, RelatertYtelseType.FRISINN);

    public static final List<RelatertYtelseType> RELATERT_YTELSE_TYPER_FOR_ANNEN_FORELDER = List.of(RelatertYtelseType.FORELDREPENGER,
        RelatertYtelseType.ENGANGSTØNAD);

    private BehandlingRelaterteYtelserMapper() {
    }

    public static List<TilgrensendeYtelser> mapFraBehandlingRelaterteYtelser(Collection<Ytelse> ytelser) {
        return ytelser.stream().map(BehandlingRelaterteYtelserMapper::lagTilgrensendeYtelse).toList();
    }

    public static RelatertYtelseType mapFraFagsakYtelseTypeTilRelatertYtelseType(FagsakYtelseType type) {
        return YTELSE_TYPE_MAP.getOrDefault(type, RelatertYtelseType.UDEFINERT);
    }

    private static TilgrensendeYtelser lagTilgrensendeYtelse(Ytelse ytelse) {
        return new TilgrensendeYtelser(ytelse);
    }

    public static TilgrensendeYtelser mapFraFagsak(Fagsak fagsak, LocalDate periodeDato) {
        var relatertYtelseType = YTELSE_TYPE_MAP.getOrDefault(fagsak.getYtelseType(), RelatertYtelseType.UDEFINERT);
        return new TilgrensendeYtelser(relatertYtelseType, periodeDato, endreTomDatoHvisLøpende(periodeDato), fagsak.getStatus().getNavn(),
            fagsak.getSaksnummer());
    }

    private static LocalDate endreTomDatoHvisLøpende(LocalDate tomDato) {
        if (Tid.TIDENES_ENDE.equals(tomDato)) {
            return null;
        }
        return tomDato;
    }

    public static List<RelaterteYtelserDto> samleYtelserBasertPåYtelseType(List<TilgrensendeYtelser> tilgrensendeYtelser,
                                                                           List<RelatertYtelseType> ytelsesTyper) {
        List<RelaterteYtelserDto> relaterteYtelserDtos = new LinkedList<>();
        for (var relatertYtelseType : ytelsesTyper) {
            relaterteYtelserDtos.add(
                new RelaterteYtelserDto(relatertYtelseType.getNavn(), sortTilgrensendeYtelser(tilgrensendeYtelser, relatertYtelseType)));
        }
        return relaterteYtelserDtos;
    }

    private static List<RelaterteYtelserDto.TilgrensendeYtelserDto> sortTilgrensendeYtelser(List<TilgrensendeYtelser> relatertYtelser,
                                                                                            RelatertYtelseType relatertYtelseType) {
        return relatertYtelser.stream()
            .filter(tilgrensendeYtelser -> relatertYtelseType.equals(tilgrensendeYtelser.relatertYtelseType()))
            .sorted()
            .map(t -> new RelaterteYtelserDto.TilgrensendeYtelserDto(t.periodeFra(), t.periodeTil(), t.statusNavn(),
                t.saksNummer() != null ? t.saksNummer().getVerdi() : null))
            .toList();
    }
}
