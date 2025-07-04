package no.nav.foreldrepenger.domene.abakus.mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.abakus.iaygrunnlag.Aktør;
import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
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
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder;
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
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class MapAktørYtelse {

    private MapAktørYtelse() {
        // skjul public constructor
    }

    static class MapFraDto {
        private InntektArbeidYtelseAggregatBuilder aggregatBuilder;

        MapFraDto(InntektArbeidYtelseAggregatBuilder aggregatBuilder) {
            this.aggregatBuilder = aggregatBuilder;
        }

        List<AktørYtelseBuilder> map(Collection<YtelserDto> dtos) {
            if (dtos == null || dtos.isEmpty()) {
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
            ytelseBuilder
                    .medYtelseGrunnlag(mapYtelseGrunnlag(ytelseDto.getGrunnlag(), ytelseBuilder.getGrunnlagBuilder()))
                    .medYtelseType(KodeverkMapper.mapYtelseTypeFraDto(ytelseDto.getYtelseType()))
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

}
