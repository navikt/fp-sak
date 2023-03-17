package no.nav.foreldrepenger.domene.abakus.mapping;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.abakus.iaygrunnlag.Aktør;
import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.Organisasjon;
import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.abakus.iaygrunnlag.PersonIdent;
import no.nav.abakus.iaygrunnlag.ytelse.v1.AnvisningDto;
import no.nav.abakus.iaygrunnlag.ytelse.v1.AnvistAndelDto;
import no.nav.abakus.iaygrunnlag.ytelse.v1.FordelingDto;
import no.nav.abakus.iaygrunnlag.ytelse.v1.YtelseDto;
import no.nav.abakus.iaygrunnlag.ytelse.v1.YtelseGrunnlagDto;
import no.nav.abakus.iaygrunnlag.ytelse.v1.YtelserDto;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvistAndel;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvistAndelBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvistBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelseBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

public class MapAktørYtelse {

    private static final Comparator<YtelseDto> COMP_YTELSE = Comparator
            .comparing(YtelseDto::getSaksnummer, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(dto -> dto.getYtelseType() == null ? null : dto.getYtelseType().getKode(), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(dto -> dto.getTemaUnderkategori() == null ? null : dto.getTemaUnderkategori().getKode(),
                    Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(dto -> dto.getPeriode().getFom(), Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(dto -> dto.getPeriode().getTom(), Comparator.nullsLast(Comparator.naturalOrder()));

    private MapAktørYtelse() {
        // skjul public constructor
    }

    static class MapFraDto {
        private InntektArbeidYtelseAggregatBuilder aggregatBuilder;

        MapFraDto(AktørId søkerAktørId, InntektArbeidYtelseAggregatBuilder aggregatBuilder) {
            this.aggregatBuilder = aggregatBuilder;
        }

        List<AktørYtelseBuilder> map(Collection<YtelserDto> dtos) {
            if ((dtos == null) || dtos.isEmpty()) {
                return Collections.emptyList();
            }
            return dtos.stream().map(this::mapAktørYtelse).toList();
        }

        private AktørYtelseBuilder mapAktørYtelse(YtelserDto dto) {
            var builder = aggregatBuilder.getAktørYtelseBuilder(tilAktørId(dto.getPerson()));
            dto.getYtelser().forEach(ytelseDto -> builder.leggTilYtelse(mapYtelse(ytelseDto)));
            return builder;
        }

        /**
         * Returnerer person sin aktørId. Denne trenger ikke være samme som søkers
         * aktørid men kan f.eks. være annen part i en sak.
         */
        private AktørId tilAktørId(PersonIdent person) {
            if (!(person instanceof AktørIdPersonident)) {
                throw new IllegalArgumentException("Støtter kun " + AktørIdPersonident.class.getSimpleName() + " her");
            }
            return new AktørId(person.getIdent());
        }

        private DatoIntervallEntitet mapPeriode(Periode periode) {
            return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
        }

        private YtelseBuilder mapYtelse(YtelseDto ytelseDto) {
            var ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty());
            var behandlingsTema = KodeverkMapper.getTemaUnderkategori(ytelseDto.getTemaUnderkategori());
            ytelseBuilder
                    .medYtelseGrunnlag(mapYtelseGrunnlag(ytelseDto.getGrunnlag(), ytelseBuilder.getGrunnlagBuilder()))
                    .medYtelseType(KodeverkMapper.mapYtelseTypeFraDto(ytelseDto.getYtelseType()))
                    .medBehandlingsTema(behandlingsTema)
                    .medKilde(KodeverkMapper.mapFagsystemFraDto(ytelseDto.getFagsystemDto()))
                    .medPeriode(mapPeriode(ytelseDto.getPeriode()))
                    .medVedtattTidspunkt(ytelseDto.getVedtattTidspunkt())
                    .medSaksnummer(ytelseDto.getSaksnummer() == null ? null : new Saksnummer(ytelseDto.getSaksnummer()))
                    .medStatus(KodeverkMapper.getFpsakRelatertYtelseTilstandForAbakusYtelseStatus(ytelseDto.getStatus()));
            ytelseDto.getAnvisninger()
                    .forEach(anvisning -> ytelseBuilder.medYtelseAnvist(mapYtelseAnvist(anvisning, ytelseBuilder.getAnvistBuilder())));
            return ytelseBuilder;
        }

        private YtelseAnvist mapYtelseAnvist(AnvisningDto anvisning, YtelseAnvistBuilder anvistBuilder) {
            if (anvisning == null) {
                return null;
            }
            if (anvisning.getAndeler() != null) {
                anvisning.getAndeler().stream()
                    .map(this::mapTilAnvistAndel)
                    .forEach(anvistBuilder::leggTilYtelseAnvistAndel);
            }

            return anvistBuilder
                    .medAnvistPeriode(mapPeriode(anvisning.getPeriode()))
                    .medBeløp(anvisning.getBeløp())
                    .medDagsats(anvisning.getDagsats())
                    .medUtbetalingsgradProsent(anvisning.getUtbetalingsgrad())
                    .build();
        }

        private YtelseAnvistAndel mapTilAnvistAndel(AnvistAndelDto a) {
            return YtelseAnvistAndelBuilder.ny().medDagsats(a.getDagsats())
                .medInntektskategori(a.getInntektskategori())
                .medRefusjonsgrad(a.getRefusjonsgrad())
                .medUtbetalingsgrad(a.getUtbetalingsgrad())
                .medArbeidsgiver(mapArbeidsgiver(a.getArbeidsgiver()))
                .medArbeidsforholdRef(a.getArbeidsforholdId() == null ? InternArbeidsforholdRef.nullRef() : InternArbeidsforholdRef.ref(a.getArbeidsforholdId()))
                .build();
        }

        private Arbeidsgiver mapArbeidsgiver(Aktør arbeidsgiver) {
            if (arbeidsgiver == null) {
                return null;
            }
            if (arbeidsgiver.getErOrganisasjon()) {
                return Arbeidsgiver.virksomhet(new OrgNummer(arbeidsgiver.getIdent()));
            }
            return Arbeidsgiver.person(new AktørId(arbeidsgiver.getIdent()));
        }

        private YtelseGrunnlag mapYtelseGrunnlag(YtelseGrunnlagDto grunnlag, YtelseGrunnlagBuilder grunnlagBuilder) {
            if (grunnlag == null) {
                return null;
            }
            grunnlagBuilder
                    .medArbeidskategori(KodeverkMapper.mapArbeidskategoriFraDto(grunnlag.getArbeidskategoriDto()))
                    .medDekningsgradProsent(grunnlag.getDekningsgradProsent())
                    .medGraderingProsent(grunnlag.getGraderingProsent())
                    .medInntektsgrunnlagProsent(grunnlag.getInntektsgrunnlagProsent())
                    .medVedtaksDagsats(grunnlag.getVedtaksDagsats())
                    .medOpprinneligIdentdato(grunnlag.getOpprinneligIdentDato());
            grunnlag.getFordeling()
                    .forEach(fordeling -> grunnlagBuilder.medYtelseStørrelse(mapYtelseStørrelse(fordeling)));
            return grunnlagBuilder.build();
        }

        private YtelseStørrelse mapYtelseStørrelse(FordelingDto fordeling) {
            if (fordeling == null) {
                return null;
            }
            var arbeidsgiver = fordeling.getArbeidsgiver();
            return YtelseStørrelseBuilder.ny()
                    .medBeløp(fordeling.getBeløp())
                    .medErRefusjon(fordeling.getErRefusjon())
                    .medHyppighet(KodeverkMapper.mapInntektPeriodeTypeFraDto(fordeling.getHyppighet()))
                    .medVirksomhet(arbeidsgiver == null ? null : new OrgNummer(arbeidsgiver.getIdent()))
                    .build();
        }

    }

    static class MapTilDto {

        private List<FordelingDto> mapFordeling(List<YtelseStørrelse> ytelseStørrelse) {
            return ytelseStørrelse.stream().map(this::tilFordeling).toList();
        }

        private YtelserDto mapTilYtelser(AktørYtelse ay) {
            var person = new AktørIdPersonident(ay.getAktørId().getId());
            return new YtelserDto(person)
                    .medYtelser(mapTilYtelser(ay.getAlleYtelser()));
        }

        private List<YtelseDto> mapTilYtelser(Collection<Ytelse> ytelser) {
            return ytelser.stream().map(this::tilYtelse).sorted(COMP_YTELSE).toList();
        }

        private YtelseGrunnlagDto mapYtelseGrunnlag(YtelseGrunnlag gr) {
            var dto = new YtelseGrunnlagDto();
            gr.getArbeidskategori().ifPresent(ak -> dto.setArbeidskategoriDto(KodeverkMapper.mapArbeidskategoriTilDto(ak)));
            gr.getOpprinneligIdentdato().ifPresent(dto::setOpprinneligIdentDato);
            gr.getDekningsgradProsent().map(Stillingsprosent::getVerdi).ifPresent(dto::setDekningsgradProsent);
            gr.getGraderingProsent().map(Stillingsprosent::getVerdi).ifPresent(dto::setGraderingProsent);
            gr.getInntektsgrunnlagProsent().map(Stillingsprosent::getVerdi).ifPresent(dto::setInntektsgrunnlagProsent);
            gr.getVedtaksDagsats().map(Beløp::getVerdi).ifPresent(dto::setVedtaksDagsats);
            dto.setFordeling(mapFordeling(gr.getYtelseStørrelse()));

            return dto;
        }

        private FordelingDto tilFordeling(YtelseStørrelse ytelseStørrelse) {
            var organisasjon = ytelseStørrelse.getVirksomhet().map(o -> new Organisasjon(o.getId())).orElse(null);
            var inntektPeriodeType = KodeverkMapper.mapInntektPeriodeTypeTilDto(ytelseStørrelse.getHyppighet());
            var beløp = ytelseStørrelse.getBeløp().getVerdi();
            return new FordelingDto(organisasjon, inntektPeriodeType == null ? no.nav.abakus.iaygrunnlag.kodeverk.InntektPeriodeType.UDEFINERT : inntektPeriodeType, beløp, ytelseStørrelse.getErRefusjon());
        }

        private YtelseDto tilYtelse(Ytelse ytelse) {
            var fagsystem = KodeverkMapper.mapFagsystemTilDto(ytelse.getKilde());
            var periode = mapPeriode(ytelse.getPeriode().getFomDato(), ytelse.getPeriode().getTomDato());
            var ytelseType = KodeverkMapper.mapYtelseTypeTilDto(ytelse.getRelatertYtelseType());
            var ytelseStatus = KodeverkMapper.getAbakusYtelseStatusForFpsakRelatertYtelseTilstand(ytelse.getStatus());
            var temaUnderkategori = KodeverkMapper.getBehandlingsTemaUnderkategori(ytelse.getBehandlingsTema());
            var dto = new YtelseDto(fagsystem, ytelseType, periode, ytelseStatus)
                    .medSaksnummer(ytelse.getSaksnummer() == null ? null : ytelse.getSaksnummer().getVerdi())
                    .medVedtattTidspunkt(ytelse.getVedtattTidspunkt())
                    .medTemaUnderkategori(temaUnderkategori);

            ytelse.getYtelseGrunnlag().map(this::mapYtelseGrunnlag).ifPresent(dto::setGrunnlag);

            var compAnvisning = Comparator
                    .comparing((AnvisningDto anv) -> anv.getPeriode().getFom(), Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(anv -> anv.getPeriode().getTom(), Comparator.nullsLast(Comparator.naturalOrder()));

            var anvisninger = ytelse.getYtelseAnvist().stream().map(this::map).sorted(compAnvisning).toList();
            dto.setAnvisninger(anvisninger);

            return dto;
        }

        private AnvisningDto map(YtelseAnvist ya) {
            var periode = new Periode(ya.getAnvistFOM(), ya.getAnvistTOM());
            var dto = new AnvisningDto(periode);
            ya.getBeløp().ifPresent(v -> dto.setBeløp(v.getVerdi()));
            ya.getDagsats().ifPresent(v -> dto.setDagsats(v.getVerdi()));
            ya.getUtbetalingsgradProsent().ifPresent(v -> dto.setUtbetalingsgrad(v.getVerdi()));
            return dto;
        }

        List<YtelserDto> map(Collection<AktørYtelse> aktørYtelser) {
            if ((aktørYtelser == null) || aktørYtelser.isEmpty()) {
                return Collections.emptyList();
            }
            return aktørYtelser.stream().map(this::mapTilYtelser).toList();
        }

        /**
         * enkelte gamle innslag fra Arena meldekort har dårlige fom/tom (tom > fom), da
         * dette nå må avledes basert på meldekort, vedtattdato, krav mottatt dato denne
         * metoden korrigerer for gamle dårlige innslag ved å sette dem i riktig
         * rekkefølge. Blir ikke 100% korrekt ifht ny utregning av fom/tom basert på
         * crazy Arena meldekort men disse sakene er avsluttet, og vi slipper å håndtere
         * dette fremover (kan fjerne denne metoden når migrering er ferdig overstått).
         * <br>
         * De dataene er som er dårlig kan finnes vha
         * <code>select * from IAY_RELATERT_YTELSE where TOM < FOM</code> Sakene
         * avdekkes gjennom: <code>
            select iyt.*, gr.*, f.* from iay_aktoer_ytelse iyt
            inner join gr_arbeid_inntekt gr on gr.register_id = iyt.inntekt_arbeid_ytelser_id
            inner join behandling b on b.id = gr.behandling_id
            inner join fagsak f on f.id = b.fagsak_id
            where iyt.id in (select aktoer_ytelse_id from iay_relatert_ytelse where tom < fom);
         * </code>
         */
        private static Periode mapPeriode(LocalDate fom, LocalDate tom) {
            if (fom.isAfter(tom)) {
                return new Periode(tom, tom); // kort ned til 1 dag
            }
            // do the right thing
            return new Periode(fom, tom);

        }

    }

}
