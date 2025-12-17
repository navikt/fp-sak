package no.nav.foreldrepenger.domene.person.pdl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Gyldighetsperiode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.OppholdstillatelsePeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.PersonstatusPeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.StatsborgerskapPeriode;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.pdl.FolkeregistermetadataResponseProjection;
import no.nav.pdl.Folkeregisterpersonstatus;
import no.nav.pdl.FolkeregisterpersonstatusResponseProjection;
import no.nav.pdl.Opphold;
import no.nav.pdl.OppholdResponseProjection;
import no.nav.pdl.Oppholdstillatelse;
import no.nav.pdl.PersonFolkeregisterpersonstatusParametrizedInput;
import no.nav.pdl.PersonOppholdParametrizedInput;
import no.nav.pdl.PersonResponseProjection;
import no.nav.pdl.PersonStatsborgerskapParametrizedInput;
import no.nav.pdl.Statsborgerskap;
import no.nav.pdl.StatsborgerskapResponseProjection;

public class AnnetPeriodisertMapper {

    private static final Map<Oppholdstillatelse, OppholdstillatelseType> TILLATELSE_FRA_FREG_OPPHOLD = Map.ofEntries(
        Map.entry(Oppholdstillatelse.PERMANENT, OppholdstillatelseType.PERMANENT),
        Map.entry(Oppholdstillatelse.MIDLERTIDIG, OppholdstillatelseType.MIDLERTIDIG));

    private AnnetPeriodisertMapper() {
    }

    static PersonResponseProjection leggTilAnnetPeriodisertQuery(PersonResponseProjection query, boolean historikk) {
        return query
            .statsborgerskap(new PersonStatsborgerskapParametrizedInput().historikk(historikk), new StatsborgerskapResponseProjection()
                .land().gyldigFraOgMed().gyldigTilOgMed().bekreftelsesdato())
            .folkeregisterpersonstatus(new PersonFolkeregisterpersonstatusParametrizedInput().historikk(historikk), new FolkeregisterpersonstatusResponseProjection()
                .status().folkeregistermetadata(new FolkeregistermetadataResponseProjection().ajourholdstidspunkt().gyldighetstidspunkt().opphoerstidspunkt()));
    }

    static PersonResponseProjection leggTilOppholdQuery(PersonResponseProjection query, boolean historikk) {
        return query
            .opphold(new PersonOppholdParametrizedInput().historikk(historikk), new OppholdResponseProjection().type().oppholdFra().oppholdTil());
    }


    static List<PersonstatusPeriode> mapPersonstatus(List<Folkeregisterpersonstatus> status, Gyldighetsperiode filter, LocalDate fødsel) {
        var personstatusPerioder = status.stream()
            .map(AnnetPeriodisertMapper::mapPersonstatus)
            .toList();
        var allegyldighetsperioder = personstatusPerioder.stream().map(PersonstatusPeriode::gyldighetsperiode).toList();
        return personstatusPerioder.stream()
            .map(p -> new PersonstatusPeriode(Gyldighetsperiode.justerForSenere(allegyldighetsperioder, p.gyldighetsperiode()), p.personstatus()))
            .filter(p -> p.gyldighetsperiode().overlapper(filter))
            .map(p -> new PersonstatusPeriode(fødselsjuster(p.gyldighetsperiode(), fødsel), p.personstatus()))
            .filter(p -> p.gyldighetsperiode() != null)
            .toList();
    }

    private static PersonstatusPeriode mapPersonstatus(Folkeregisterpersonstatus status) {
        var ajourFom = status.getFolkeregistermetadata().getAjourholdstidspunkt();
        var gyldigFom = status.getFolkeregistermetadata().getGyldighetstidspunkt();
        var brukFom = gyldigFom != null ? gyldigFom : ajourFom;
        var periode = Gyldighetsperiode.fraDates(brukFom, status.getFolkeregistermetadata().getOpphoerstidspunkt());
        return new PersonstatusPeriode(periode, PersonstatusType.fraFregPersonstatus(status.getStatus()));
    }


    static List<StatsborgerskapPeriode> mapStatsborgerskap(List<Statsborgerskap> statsborgerskap, Gyldighetsperiode filter, LocalDate fødsel) {
        return statsborgerskap.stream()
            .filter(s -> s.getLand() != null)
            .map(AnnetPeriodisertMapper::mapStatsborgerskap)
            .filter(p -> p.gyldighetsperiode().overlapper(filter))
            .map(p -> new StatsborgerskapPeriode(fødselsjuster(p.gyldighetsperiode(), fødsel), p.statsborgerskap()))
            .filter(p -> p.gyldighetsperiode() != null)
            .toList();
    }

    private static StatsborgerskapPeriode mapStatsborgerskap(Statsborgerskap statsborgerskap) {
        var gyldigTil = Optional.ofNullable(statsborgerskap.getGyldigTilOgMed())
            .map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
        var gyldigFra = Optional.ofNullable(statsborgerskap.getGyldigFraOgMed())
            .or(() -> Optional.ofNullable(statsborgerskap.getBekreftelsesdato()))
            .map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
        var land = Landkoder.fraKodeDefaultUkjent(statsborgerskap.getLand());
        return new StatsborgerskapPeriode(Gyldighetsperiode.innenfor(gyldigFra, gyldigTil), land);
    }

    static List<OppholdstillatelsePeriode> mapOpphold(List<Opphold> opphold, Gyldighetsperiode filter, LocalDate fødsel) {
        return opphold.stream()
            .filter(AnnetPeriodisertMapper::relevantOppholdstillatelse)
            .map(AnnetPeriodisertMapper::mapOpphold)
            .filter(p -> p.gyldighetsperiode().overlapper(filter))
            .map(p -> new OppholdstillatelsePeriode(AnnetPeriodisertMapper.fødselsjuster(p.gyldighetsperiode(), fødsel), p.tillatelse()))
            .filter(p -> p.gyldighetsperiode() != null)
            .toList();
    }

    private static boolean relevantOppholdstillatelse(Opphold opphold) {
        return Oppholdstillatelse.PERMANENT.equals(opphold.getType()) || Oppholdstillatelse.MIDLERTIDIG.equals(opphold.getType());

    }

    private static OppholdstillatelsePeriode mapOpphold(Opphold opphold) {
        var type = TILLATELSE_FRA_FREG_OPPHOLD.get(opphold.getType());
        var gyldigTil = Optional.ofNullable(opphold.getOppholdTil()).map(til -> LocalDate.parse(til, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
        var gyldigFra = Optional.ofNullable(opphold.getOppholdFra()).map(fra -> LocalDate.parse(fra, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
        return new OppholdstillatelsePeriode(Gyldighetsperiode.innenfor(gyldigFra, gyldigTil), type);
    }

    static Gyldighetsperiode fødselsjuster(Gyldighetsperiode gyldighetsperiode, LocalDate fødselsdato) {
        if (fødselsdato != null && gyldighetsperiode.fom().isBefore(fødselsdato)) {
            return gyldighetsperiode.tom().isBefore(fødselsdato) ? null : Gyldighetsperiode.innenfor(fødselsdato, gyldighetsperiode.tom());
        } else {
            return gyldighetsperiode;
        }
    }

}
