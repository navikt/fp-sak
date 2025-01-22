package no.nav.foreldrepenger.domene.mappers.fra_entitet_til_modell;

import static org.assertj.core.api.Assertions.assertThat;

import no.nav.foreldrepenger.domene.mappers.fra_entitet_til_domene.FraEntitetTilBehandlingsmodellMapper;
import no.nav.foreldrepenger.domene.modell.kodeverk.SammenligningsgrunnlagType;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;

class FraEntitetTilBehandlingsmodellMapperTest {

    @Test
    void tester_mapping_beregningsgrunnlag() {
        var bg = bgFraJson();
        var mappetGrunnlag = FraEntitetTilBehandlingsmodellMapper.mapBeregningsgrunnlagGrunnlag(bg);

        assertThat(mappetGrunnlag).isNotNull();

        // Assert registeraktiviteter
        var faktiskRegAkt = bg.getRegisterAktiviteter();
        var mappetRegAkt = mappetGrunnlag.getRegisterAktiviteter();
        assertThat(mappetRegAkt).isNotNull();
        assertThat(mappetRegAkt.getSkjæringstidspunktOpptjening()).isEqualTo(faktiskRegAkt.getSkjæringstidspunktOpptjening());
        assertThat(mappetRegAkt.getBeregningAktiviteter()).hasSameSizeAs(faktiskRegAkt.getBeregningAktiviteter());

        assertThat(mappetRegAkt.getBeregningAktiviteter().get(0).getPeriode()).isEqualTo(faktiskRegAkt.getBeregningAktiviteter().get(0).getPeriode());
        assertThat(mappetRegAkt.getBeregningAktiviteter().get(0).getArbeidsforholdRef()).isEqualTo(faktiskRegAkt.getBeregningAktiviteter().get(0).getArbeidsforholdRef());
        assertThat(mappetRegAkt.getBeregningAktiviteter().get(0).getArbeidsgiver()).isEqualTo(faktiskRegAkt.getBeregningAktiviteter().get(0).getArbeidsgiver());
        assertThat(mappetRegAkt.getBeregningAktiviteter().get(0).getOpptjeningAktivitetType()).isEqualTo(faktiskRegAkt.getBeregningAktiviteter().get(0).getOpptjeningAktivitetType());

        assertThat(mappetRegAkt.getBeregningAktiviteter().get(1).getPeriode()).isEqualTo(faktiskRegAkt.getBeregningAktiviteter().get(1).getPeriode());
        assertThat(mappetRegAkt.getBeregningAktiviteter().get(1).getArbeidsforholdRef()).isEqualTo(faktiskRegAkt.getBeregningAktiviteter().get(1).getArbeidsforholdRef());
        assertThat(mappetRegAkt.getBeregningAktiviteter().get(1).getArbeidsgiver()).isEqualTo(faktiskRegAkt.getBeregningAktiviteter().get(1).getArbeidsgiver());
        assertThat(mappetRegAkt.getBeregningAktiviteter().get(1).getOpptjeningAktivitetType()).isEqualTo(faktiskRegAkt.getBeregningAktiviteter().get(1).getOpptjeningAktivitetType());

        // Assert oversyrte aktiviteter
        var faktiskOsAkt = bg.getOverstyrteEllerRegisterAktiviteter();
        var mappetOsAkt = mappetGrunnlag.getOverstyrteEllerRegisterAktiviteter();
        assertThat(mappetOsAkt).isNotNull();
        assertThat(mappetOsAkt.getSkjæringstidspunktOpptjening()).isEqualTo(faktiskOsAkt.getSkjæringstidspunktOpptjening());
        assertThat(mappetOsAkt.getBeregningAktiviteter()).hasSameSizeAs(faktiskOsAkt.getBeregningAktiviteter());

        assertThat(mappetOsAkt.getBeregningAktiviteter().get(0).getPeriode()).isEqualTo(faktiskOsAkt.getBeregningAktiviteter().get(0).getPeriode());
        assertThat(mappetOsAkt.getBeregningAktiviteter().get(0).getArbeidsforholdRef()).isEqualTo(faktiskOsAkt.getBeregningAktiviteter().get(0).getArbeidsforholdRef());
        assertThat(mappetOsAkt.getBeregningAktiviteter().get(0).getArbeidsgiver()).isEqualTo(faktiskOsAkt.getBeregningAktiviteter().get(0).getArbeidsgiver());
        assertThat(mappetOsAkt.getBeregningAktiviteter().get(0).getOpptjeningAktivitetType()).isEqualTo(faktiskOsAkt.getBeregningAktiviteter().get(0).getOpptjeningAktivitetType());

        // Assert beregningsgrunnlag
        var faktiskBG = bg.getBeregningsgrunnlag().get();
        var mappetBG = mappetGrunnlag.getBeregningsgrunnlag().get();
        assertThat(mappetBG).isNotNull();
        assertThat(mappetBG.getSkjæringstidspunkt()).isEqualTo(faktiskBG.getSkjæringstidspunkt());
        assertThat(mappetBG.getGrunnbeløp()).isEqualTo(faktiskBG.getGrunnbeløp());
        assertThat(mappetBG.getHjemmel()).isEqualTo(faktiskBG.getHjemmel());
        assertThat(mappetBG.getAktivitetStatuser()).hasSameSizeAs(faktiskBG.getAktivitetStatuser());

        // Aktivitetstatus
        assertThat(mappetBG.getAktivitetStatuser().get(0).getAktivitetStatus()).isEqualTo(faktiskBG.getAktivitetStatuser().get(0).getAktivitetStatus());
        assertThat(mappetBG.getAktivitetStatuser().get(0).getHjemmel()).isEqualTo(faktiskBG.getAktivitetStatuser().get(0).getHjemmel());

        // Sammenligningsgrunnlag
        var faktiskSG = faktiskBG.getSammenligningsgrunnlag().get();
        var mappetSG = mappetBG.getSammenligningsgrunnlagPrStatusListe().getFirst();
        assertThat(mappetSG).isNotNull();
        assertThat(mappetSG.getRapportertPrÅr()).isEqualByComparingTo(faktiskSG.getRapportertPrÅr());
        assertThat(mappetSG.getAvvikPromille()).isEqualTo(faktiskSG.getAvvikPromille().longValue());
        assertThat(mappetSG.getSammenligningsperiodeFom()).isEqualTo(faktiskSG.getSammenligningsperiodeFom());
        assertThat(mappetSG.getSammenligningsperiodeTom()).isEqualTo(faktiskSG.getSammenligningsperiodeTom());
        assertThat(mappetSG.getSammenligningsgrunnlagType()).isEqualTo(SammenligningsgrunnlagType.SAMMENLIGNING_AT_FL);

        // Beregningsgrunnlagsperiode
        assertThat(faktiskBG.getBeregningsgrunnlagPerioder()).hasSize(mappetBG.getBeregningsgrunnlagPerioder().size());
        var faktiskPeriode = faktiskBG.getBeregningsgrunnlagPerioder().get(0);
        var mappetPeriode = mappetBG.getBeregningsgrunnlagPerioder().get(0);
        assertThat(mappetPeriode.getPeriode()).isEqualTo(faktiskPeriode.getPeriode());
        assertThat(mappetPeriode.getAvkortetPrÅr()).isEqualByComparingTo(faktiskPeriode.getAvkortetPrÅr());
        assertThat(mappetPeriode.getRedusertPrÅr()).isEqualByComparingTo(faktiskPeriode.getRedusertPrÅr());
        assertThat(mappetPeriode.getBruttoPrÅr()).isEqualByComparingTo(faktiskPeriode.getBruttoPrÅr());
        assertThat(mappetPeriode.getBeregnetPrÅr()).isEqualByComparingTo(faktiskPeriode.getBeregnetPrÅr());
        assertThat(mappetPeriode.getBeregningsgrunnlagPeriodeFom()).isEqualTo(faktiskPeriode.getBeregningsgrunnlagPeriodeFom());
        assertThat(mappetPeriode.getBeregningsgrunnlagPeriodeTom()).isEqualTo(faktiskPeriode.getBeregningsgrunnlagPeriodeTom());
        assertThat(mappetPeriode.getPeriodeÅrsaker()).isEmpty();
        assertThat(mappetPeriode.getDagsats()).isEqualTo(faktiskPeriode.getDagsats());
        assertThat(mappetPeriode.getBeregningsgrunnlagPrStatusOgAndelList()).hasSameSizeAs(faktiskPeriode.getBeregningsgrunnlagPrStatusOgAndelList());

        // Beregningsgrunnlagandel
        var mappetAndel = mappetPeriode.getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        var faktiskAndel = faktiskPeriode.getBeregningsgrunnlagPrStatusOgAndelList().get(0);

        assertThat(mappetAndel).isNotNull();
        assertThat(mappetAndel.getDagsats()).isEqualTo(faktiskAndel.getDagsats());
        assertThat(mappetAndel.getAndelsnr()).isEqualTo(faktiskAndel.getAndelsnr());
        assertThat(mappetAndel.getAktivitetStatus()).isEqualTo(faktiskAndel.getAktivitetStatus());
        assertThat(mappetAndel.getArbeidsforholdType()).isEqualTo(faktiskAndel.getArbeidsforholdType());
        assertThat(mappetAndel.getInntektskategori()).isEqualTo(faktiskAndel.getInntektskategori());
        assertThat(mappetAndel.getLagtTilAvSaksbehandler()).isEqualTo(faktiskAndel.erLagtTilAvSaksbehandler());
        assertThat(mappetAndel.getBeregningsperiodeFom()).isEqualTo(faktiskAndel.getBeregningsperiodeFom());
        assertThat(mappetAndel.getBeregningsperiodeTom()).isEqualTo(faktiskAndel.getBeregningsperiodeTom());
        assertThat(mappetAndel.getDagsatsArbeidsgiver()).isEqualTo(faktiskAndel.getDagsatsArbeidsgiver());
        assertThat(mappetAndel.getDagsatsBruker()).isEqualTo(faktiskAndel.getDagsatsBruker());
        assertThat(mappetAndel.getPgi1()).isEqualTo(faktiskAndel.getPgi1());
        assertThat(mappetAndel.getPgi2()).isEqualTo(faktiskAndel.getPgi2());
        assertThat(mappetAndel.getPgi3()).isEqualTo(faktiskAndel.getPgi3());
        assertThat(mappetAndel.getPgiSnitt()).isEqualTo(faktiskAndel.getPgiSnitt());
        assertThat(mappetAndel.getBruttoPrÅr()).isEqualByComparingTo(faktiskAndel.getBruttoPrÅr());
        assertThat(mappetAndel.getOverstyrtPrÅr()).isEqualByComparingTo(faktiskAndel.getOverstyrtPrÅr());
        assertThat(mappetAndel.getFordeltPrÅr()).isEqualTo(faktiskAndel.getFordeltPrÅr());
        assertThat(mappetAndel.getBeregnetPrÅr()).isEqualByComparingTo(faktiskAndel.getBeregnetPrÅr());
        assertThat(mappetAndel.getAvkortetPrÅr()).isEqualByComparingTo(faktiskAndel.getAvkortetPrÅr());
        assertThat(mappetAndel.getAvkortetBrukersAndelPrÅr()).isEqualByComparingTo(faktiskAndel.getAvkortetBrukersAndelPrÅr());
        assertThat(mappetAndel.getAvkortetRefusjonPrÅr()).isEqualByComparingTo(faktiskAndel.getAvkortetRefusjonPrÅr());
        assertThat(mappetAndel.getRedusertPrÅr()).isEqualByComparingTo(faktiskAndel.getRedusertPrÅr());
        assertThat(mappetAndel.getRedusertBrukersAndelPrÅr()).isEqualByComparingTo(faktiskAndel.getRedusertBrukersAndelPrÅr());
        assertThat(mappetAndel.getRedusertRefusjonPrÅr()).isEqualByComparingTo(faktiskAndel.getRedusertRefusjonPrÅr());

        // Arbeidsforhold
        var mappetArbfor = mappetAndel.getBgAndelArbeidsforhold().get();
        var faktiskArbfor = faktiskAndel.getBgAndelArbeidsforhold().get();
        assertThat(mappetArbfor).isNotNull();
        assertThat(mappetArbfor.getArbeidsgiver()).isEqualTo(faktiskArbfor.getArbeidsgiver());
        assertThat(mappetArbfor.getArbeidsperiodeFom()).isEqualTo(faktiskArbfor.getArbeidsperiodeFom());
        assertThat(mappetArbfor.getArbeidsperiodeTom()).isEqualTo(faktiskArbfor.getArbeidsperiodeTom());
        assertThat(mappetArbfor.getArbeidsforholdRef()).isEqualTo(faktiskArbfor.getArbeidsforholdRef());
        assertThat(mappetArbfor.getNaturalytelseBortfaltPrÅr()).isEmpty();
        assertThat(mappetArbfor.getNaturalytelseTilkommetPrÅr()).isEmpty();
        assertThat(mappetArbfor.getRefusjonskravPrÅr()).isEqualTo(faktiskArbfor.getGjeldendeRefusjon());
    }

    private BeregningsgrunnlagGrunnlagEntitet bgFraJson() {
        var json = """
            {
              "opprettetAv" : "vtp",
              "opprettetTidspunkt" : "2022-06-30T12:13:05.9612588",
              "beregningsgrunnlag" : {
                "opprettetAv" : "vtp",
                "opprettetTidspunkt" : "2022-06-30T12:13:05.9591278",
                "id" : 1000224,
                "versjon" : 0,
                "skjæringstidspunkt" : "2022-01-30",
                "aktivitetStatuser" : [ {
                  "opprettetAv" : "vtp",
                  "opprettetTidspunkt" : "2022-06-30T12:13:05.9591278",
                  "id" : 1000224,
                  "versjon" : 0,
                  "aktivitetStatus" : "AT",
                  "hjemmel" : "F_14_7_8_30"
                } ],
                "beregningsgrunnlagPerioder" : [ {
                  "opprettetAv" : "vtp",
                  "opprettetTidspunkt" : "2022-06-30T12:13:05.9591278",
                  "beregningsgrunnlagPrStatusOgAndelList" : [ {
                    "opprettetAv" : "vtp",
                    "opprettetTidspunkt" : "2022-06-30T12:13:05.9591278",
                    "andelsnr" : 1,
                    "aktivitetStatus" : "AT",
                    "beregningsperiode" : {
                      "fomDato" : "2021-10-01",
                      "tomDato" : "2021-12-31"
                    },
                    "arbeidsforholdType" : "ARBEID",
                    "bruttoPrÅr" : 500000,
                    "overstyrtPrÅr" : 500000,
                    "avkortetPrÅr" : 500000.00,
                    "redusertPrÅr" : 500000.00,
                    "beregnetPrÅr" : 480000,
                    "maksimalRefusjonPrÅr" : 0,
                    "avkortetRefusjonPrÅr" : 0,
                    "redusertRefusjonPrÅr" : 0,
                    "avkortetBrukersAndelPrÅr" : 500000.00,
                    "redusertBrukersAndelPrÅr" : 500000.00,
                    "dagsatsBruker" : 1923,
                    "dagsatsArbeidsgiver" : 0,
                    "årsbeløpFraTilstøtendeYtelse" : { },
                    "fastsattAvSaksbehandler" : false,
                    "inntektskategori" : "ARBEIDSTAKER",
                    "inntektskategoriAutomatiskFordeling" : "ARBEIDSTAKER",
                    "kilde" : "PROSESS_START",
                    "bgAndelArbeidsforhold" : {
                      "opprettetAv" : "vtp",
                      "opprettetTidspunkt" : "2022-06-30T12:13:05.9591278",
                      "arbeidsgiver" : {
                        "arbeidsgiverOrgnr" : "342352362"
                      },
                      "arbeidsforholdRef" : { },
                      "arbeidsperiodeFom" : "2021-08-30",
                      "arbeidsperiodeTom" : "2022-01-29"
                    },
                    "beregningsgrunnlagArbeidstakerAndel" : {
                      "opprettetAv" : "vtp",
                      "opprettetTidspunkt" : "2022-06-30T12:13:05.9591278"
                    }
                  } ],
                  "periode" : {
                    "fomDato" : "2022-01-30",
                    "tomDato" : "9999-12-31"
                  },
                  "bruttoPrÅr" : 500000,
                  "avkortetPrÅr" : 500000.00,
                  "redusertPrÅr" : 500000.00,
                  "dagsats" : 1923
                } ],
                "sammenligningsgrunnlag" : {
                  "opprettetAv" : "vtp",
                  "opprettetTidspunkt" : "2022-06-30T12:13:05.9612588",
                  "sammenligningsperiode" : {
                    "fomDato" : "2021-01-01",
                    "tomDato" : "2021-12-31"
                  },
                  "rapportertPrÅr" : 200000,
                  "avvikPromille" : 1400
                },
                "grunnbeløp" : {
                  "verdi" : 111477
                },
                "overstyrt" : false
              },
              "registerAktiviteter" : {
                "opprettetAv" : "vtp",
                "opprettetTidspunkt" : "2022-06-30T12:13:05.9591278",
                "aktiviteter" : [ {
                  "opprettetAv" : "vtp",
                  "opprettetTidspunkt" : "2022-06-30T12:13:05.9591278",
                  "id" : 1000297,
                  "versjon" : 0,
                  "periode" : {
                    "fomDato" : "2021-08-30",
                    "tomDato" : "2022-01-29"
                  },
                  "arbeidsgiver" : {
                    "arbeidsgiverOrgnr" : "342352362"
                  },
                  "arbeidsforholdRef" : { },
                  "opptjeningAktivitetType" : "ARBEID"
                }, {
                  "opprettetAv" : "vtp",
                  "opprettetTidspunkt" : "2022-06-30T12:13:05.9591278",
                  "periode" : {
                    "fomDato" : "2022-01-30",
                    "tomDato" : "2022-06-02"
                  },
                  "arbeidsforholdRef" : { },
                  "opptjeningAktivitetType" : "DAGPENGER"
                } ],
                "skjæringstidspunktOpptjening" : "2022-05-26"
              },
              "overstyringer" : {
                "opprettetAv" : "vtp",
                "opprettetTidspunkt" : "2022-06-30T12:13:05.9591278",
                "overstyringer" : [ {
                  "opprettetAv" : "vtp",
                  "opprettetTidspunkt" : "2022-06-30T12:13:05.9591278",
                  "periode" : {
                    "fomDato" : "2022-01-30",
                    "tomDato" : "2022-06-02"
                  },
                  "handlingType" : "IKKE_BENYTT",
                  "opptjeningAktivitetType" : "DAGPENGER"
                }, {
                  "opprettetAv" : "vtp",
                  "opprettetTidspunkt" : "2022-06-30T12:13:05.9591278",
                  "periode" : {
                    "fomDato" : "2021-08-30",
                    "tomDato" : "2022-01-29"
                  },
                  "arbeidsgiver" : {
                    "arbeidsgiverOrgnr" : "342352362"
                  },
                  "handlingType" : "BENYTT",
                  "opptjeningAktivitetType" : "ARBEID"
                } ]
              },
              "aktiv" : true,
              "beregningsgrunnlagTilstand" : "FASTSATT"
            }""";
        return StandardJsonConfig.fromJson(json, BeregningsgrunnlagGrunnlagEntitet.class);
    }
}
