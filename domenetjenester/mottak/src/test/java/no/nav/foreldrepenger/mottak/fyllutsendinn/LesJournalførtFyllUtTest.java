package no.nav.foreldrepenger.mottak.fyllutsendinn;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class LesJournalførtFyllUtTest {

    @Test
    void les_svangerskapspenger_eksempel140410() throws Exception {
        // eksempel140410.json - Svangerskapspenger søknad
        try (var inputStream = getClass().getResourceAsStream("/fyllutsendinn/eksempel140410.json")) {
            FormSubmission<Nav140410Data> submission = DefaultJsonMapper.getJsonMapper()
                .readerFor(new TypeReference<FormSubmission<Nav140410Data>>() {})
                .readValue(inputStream);

            // Verify top-level structure
            assertThat(submission).isNotNull();
            assertThat(submission.getLanguage()).isEqualTo("nb");
            assertThat(submission.getData()).isNotNull();
            assertThat(submission.getData().getData()).isNotNull();

            // Verify data is correct type
            var data = submission.getData().getData();
            assertThat(data).isInstanceOf(Nav140410Data.class);

            // Verify personal information
            assertThat(data.getDineOpplysninger1()).isNotNull();
            assertThat(data.getDineOpplysninger1().getFornavn()).isEqualTo("hjk");
            assertThat(data.getDineOpplysninger1().getEtternavn()).isEqualTo("Kåre");
            assertThat(data.getDineOpplysninger1().getIdentitet()).isNotNull();
            assertThat(data.getDineOpplysninger1().getIdentitet().getHarDuFodselsnummer()).isEqualTo(JaNei.JA);
            assertThat(data.getDineOpplysninger1().getIdentitet().getIdentitetsnummer()).isEqualTo("21831999753");

            // Verify svangerskapspenger specific fields
            assertThat(data.getHarDuBoddINorgeDeSiste12Manedene()).isEqualTo(Nav140410Data.HarDuBoddINorgeDeSiste12Manedene.JEG_HAR_KUN_BODD_I_NORGE);
            assertThat(data.getHvorSkalDuBoDeNeste12Manedene()).isEqualTo(Nav140410Data.HvorSkalDuBoDeNeste12Manedene.JEG_SKAL_KUN_BO_I_NORGE);
            assertThat(data.getHarDuHattJobbIEuEosLandDeSiste10Manedene()).isEqualTo(JaNei.JA);
            assertThat(data.getHvilketLandJobbetDuI()).isEqualTo(Nav140410Data.HvilketLandJobbetDuI.ESTLAND);
            assertThat(data.getOppgiNavnetPaArbeidsgiveren()).isEqualTo("fghjk");
            assertThat(data.getHarDuJobbetOgHattInntektSomFrilanserDeSiste10Manedene()).isEqualTo(JaNei.NEI);
            assertThat(data.getHarDuJobbetOgHattInntektSomSelvstendigNaeringsdrivendeDeSiste10Manedene()).isEqualTo(JaNei.NEI);

            // Verify dates
            assertThat(data.getNarHarDuTermindatoDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 26));
            assertThat(data.getErBarnetFodt()).isEqualTo(JaNei.JA);
            assertThat(data.getNarBleBarnetFodtDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 17));
            assertThat(data.getFraHvilkenDatoHarDuBehovForSvangerskapspengerDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 10));
            assertThat(data.getFraHvilkenDatoHarDuHattJobbIEuEosLandDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 4));
            assertThat(data.getErDetEnJobbDuHarPerIDag()).isEqualTo(JaNei.NEI);
            assertThat(data.getTilHvilkenDatoHarDuHattJobbIEuEosLandDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 19));

            // Verify work situation
            assertThat(data.getHvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger()).isNotNull();
            assertThat(data.getHvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger().isJegKanFortsetteMedSammeStillingsprosent()).isTrue();
            assertThat(data.getHvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger().isJegKanFortsetteMedRedusertArbeidstid()).isFalse();
            assertThat(data.getHvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger().isJegKanIkkeFortsetteAJobbe()).isFalse();

            // Verify attachments
            assertThat(submission.getData().getAttachments()).isNotNull();
            assertThat(submission.getData().getAttachments()).hasSize(3);

            var personalIdAttachment = submission.getData().getAttachments().get(0);
            assertThat(personalIdAttachment.getAttachmentId()).isEqualTo("personal-id");
            assertThat(personalIdAttachment.getType()).isEqualTo("personal-id");
            assertThat(personalIdAttachment.getValue()).isEqualTo("norwegianPassport");
            assertThat(personalIdAttachment.getFiles()).hasSize(1);
            assertThat(personalIdAttachment.getFiles().get(0).getFileName()).isEqualTo("delme-4 (1).pdf");
            assertThat(personalIdAttachment.getFiles().get(0).getSize()).isEqualTo(1170049L);
        }
    }

    @Test
    void les_foreldrepenger_far_aleneomsorg_eksempel140509() throws Exception {
        // eksempel140509.json - Foreldrepenger far med aleneomsorg
        try (var inputStream = getClass().getResourceAsStream("/fyllutsendinn/eksempel140509.json")) {
            FormSubmission<Nav140509Data> submission = DefaultJsonMapper.getJsonMapper()
                .readerFor(new TypeReference<FormSubmission<Nav140509Data>>() {})
                .readValue(inputStream);

            // Verify top-level structure
            assertThat(submission).isNotNull();
            assertThat(submission.getLanguage()).isEqualTo("nb");
            assertThat(submission.getData()).isNotNull();
            assertThat(submission.getData().getData()).isNotNull();

            // Verify data is correct type
            var data = submission.getData().getData();
            assertThat(data).isInstanceOf(Nav140509Data.class);

            // Verify confirmations
            assertThat(data.isJegVilSvareSaGodtJegKanPaSporsmaleneISoknaden()).isTrue();
            assertThat(data.isJegVilSvareSaGodtJegKanPaSporsmaleneISoknaden()).isTrue();

            // Verify personal information
            assertThat(data.getDineOpplysninger1()).isNotNull();
            assertThat(data.getDineOpplysninger1().getFornavn()).isEqualTo("Bob");
            assertThat(data.getDineOpplysninger1().getEtternavn()).isEqualTo("Kåre");
            assertThat(data.getDineOpplysninger1().getIdentitet()).isNotNull();
            assertThat(data.getDineOpplysninger1().getIdentitet().getHarDuFodselsnummer()).isEqualTo(JaNei.JA);
            assertThat(data.getDineOpplysninger1().getIdentitet().getIdentitetsnummer()).isEqualTo("21831999753");

            // Verify applicant role and benefit type
            assertThat(data.getHvemErDu()).isEqualTo(Nav140509Data.HvemErDu.FAR);
            assertThat(data.getHvorLangPeriodeMedForeldrepengerOnskerDu()).isEqualTo(Nav140509Data.HvorLangPeriodeMedForeldrepengerOnskerDu._100_PROSENT_FORELDREPENGER);

            // Verify birth information
            assertThat(data.getErBarnetFodt()).isEqualTo(JaNei.JA);
            assertThat(data.getBarnetErFodt()).isNotNull();
            assertThat(data.getBarnetErFodt().getHvorMangeBarnFikkDu()).isEqualTo(2);
            assertThat(data.getBarnetErFodt().getNarBleDetEldsteBarnetFodtDdMmAaaa1()).isEqualTo(LocalDate.of(2026, 3, 2));
            assertThat(data.getBarnetErFodt().getNarVarTermindatoDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 2));
            assertThat(data.getBarnetErFodt().getBleBarnaFodtINorge()).isEqualTo(JaNei.JA);

            // Verify other parent information (aleneomsorg)
            assertThat(data.getKanDuGiOssNavnetPaDenAndreForelderen()).isEqualTo(JaNei.NEI);

            // Verify benefit period
            assertThat(data.getPeriodeMedforeldrepengerVedAleneomsorgFarMedmor()).hasSize(1);
            var periode = data.getPeriodeMedforeldrepengerVedAleneomsorgFarMedmor().get(0);
            assertThat(periode.getDatoFraOgMedDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 2));
            assertThat(periode.getDatoTilOgMedDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 13));
            assertThat(periode.getSkalDuKombinereForeldrepengeneMedDelvisArbeid()).isEqualTo(JaNei.NEI);

            // Verify residence information
            assertThat(data.getHvorSkalDuBoDeNeste12Manedene()).isEqualTo(Nav140509Data.HvorSkalDuBoDeNeste12Manedene.KUN_BO_I_NORGE);
            assertThat(data.getHvorHarDuBoddDeSiste12Manedene()).isEqualTo(Nav140509Data.HvorHarDuBoddDeSiste12Manedene.KUN_BODD_I_NORGE);

            // Verify income sources
            assertThat(data.getHarDuArbeidsforholdINorge()).isEqualTo(JaNei.NEI);
            assertThat(data.getHarDuJobbetOgHattInntektSomFrilanserDeSiste10Manedene()).isEqualTo(JaNei.NEI);
            assertThat(data.getHarDuJobbetOgHattInntektSomSelvstendigNaeringsdrivendeDeSiste10Manedene()).isEqualTo(JaNei.NEI);

            // Verify attachments
            assertThat(submission.getData().getAttachments()).isNotNull();
            assertThat(submission.getData().getAttachments()).hasSize(3);

            var firstAttachment = submission.getData().getAttachments().get(0);
            assertThat(firstAttachment.getType()).isEqualTo("personal-id");
            assertThat(firstAttachment.getValue()).isEqualTo("norwegianPassport");
            assertThat(firstAttachment.getFiles()).hasSize(1);
            assertThat(firstAttachment.getFiles().get(0).getFileName()).isEqualTo("delme-3 (1).pdf");
            assertThat(firstAttachment.getFiles().get(0).getSize()).isEqualTo(385544L);
        }
    }

    @Test
    void les_foreldrepenger_mor_med_utsettelse_eksempel141605() throws Exception {
        // eksempel141605.json - Foreldrepenger mor med utsettelse og gradering
        try (var inputStream = getClass().getResourceAsStream("/fyllutsendinn/eksempel141605.json")) {
            FormSubmission<Nav141605Data> submission = DefaultJsonMapper.getJsonMapper()
                .readerFor(new TypeReference<FormSubmission<Nav141605Data>>() {})
                .readValue(inputStream);

            // Verify top-level structure
            assertThat(submission).isNotNull();
            assertThat(submission.getLanguage()).isEqualTo("nb");
            assertThat(submission.getData()).isNotNull();
            assertThat(submission.getData().getData()).isNotNull();

            // Verify data is correct type
            var data = submission.getData().getData();
            assertThat(data).isInstanceOf(Nav141605Data.class);

            // Verify confirmations
            assertThat(data.isJegVilSvareSaGodtJegKanPaSporsmaleneISoknaden2()).isTrue();
            assertThat(data.isJegBekrefterAtjegSkalHaOmsorgenForBarnetIPeriodeneJegSokerForeldrepenger()).isTrue();

            // Verify personal information
            assertThat(data.getDineOpplysninger1()).isNotNull();
            assertThat(data.getDineOpplysninger1().getFornavn()).isEqualTo("Bob");
            assertThat(data.getDineOpplysninger1().getEtternavn()).isEqualTo("Kåre");
            assertThat(data.getDineOpplysninger1().getIdentitet()).isNotNull();
            assertThat(data.getDineOpplysninger1().getIdentitet().getHarDuFodselsnummer()).isEqualTo(JaNei.JA);
            assertThat(data.getDineOpplysninger1().getIdentitet().getIdentitetsnummer()).isEqualTo("21831999753");

            // Verify applicant role
            assertThat(data.getHvemErDu()).isEqualTo(Nav141605Data.HvemErDu.MOR);
            assertThat(data.getErDuAleneOmOmsorgenAvBarnet()).isEqualTo(JaNei.NEI);

            // Verify what is being applied for
            assertThat(data.getHvaSokerDuOm()).isNotNull();
            assertThat(data.getHvaSokerDuOm().isPeriodeMedForeldrepenger()).isTrue();
            assertThat(data.getHvaSokerDuOm().isPeriodeUtenForeldrepenger()).isFalse();
            assertThat(data.getHvaSokerDuOm().isUtsettelseForste6UkeneEtterFodsel()).isTrue();

            // Verify benefit periods for mor
            assertThat(data.getMor()).isNotNull();
            assertThat(data.getMor().getHvilkenPeriodeSkalDuTaUtMor()).isNotNull();
            var hvilkenPeriode = data.getMor().getHvilkenPeriodeSkalDuTaUtMor();
            assertThat(hvilkenPeriode.isForeldrepengerForFodsel()).isTrue();
            assertThat(hvilkenPeriode.isModrekvote()).isFalse();
            assertThat(hvilkenPeriode.isFellesperiode()).isTrue();
            assertThat(hvilkenPeriode.isOverforingAvAnnenForeldersKvote()).isFalse();

            // Verify other parent information
            assertThat(data.getHarDenAndreForelderenRettTilForeldrepenger()).isEqualTo(JaNei.JA);
            assertThat(data.getHarDuOrientertDenAndreForelderenOmSoknadenDin()).isEqualTo(JaNei.JA);

            // Verify utsettelse periods
            assertThat(data.getPerioderMedUtsettelseForste6UkerEtterFodsel()).hasSize(1);
            var utsettelse = data.getPerioderMedUtsettelseForste6UkerEtterFodsel().get(0);
            assertThat(utsettelse.getDatoFraOgMedDdMmAaaa1()).isEqualTo(LocalDate.of(2026, 3, 10));
            assertThat(utsettelse.getDatoTilOgMedDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 27));
            assertThat(utsettelse.getHvorforSkalDuUtsetteForeldrepenger()).isEqualTo(Nav141605Data.HvorforSkalDuUtsetteForeldrepenger.JEG_ER_FOR_SYK_TIL_A_TA_MEG_AV_BARNET);

            // Verify foreldrepenger før fødsel
            assertThat(data.getForeldrepengerForFodsel()).hasSize(1);
            var forFodselPeriode = data.getForeldrepengerForFodsel().get(0);
            assertThat(forFodselPeriode.getForeldrepengerFraOgMedDatoDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 3));
            assertThat(forFodselPeriode.getForeldrepengerTilOgMedDatoDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 26));

            // Verify fellesperiode with gradering
            assertThat(data.getFellesperiodeMor()).hasSize(1);
            var fellesperiode = data.getFellesperiodeMor().get(0);
            assertThat(fellesperiode.getFellesperiodeFraOgMedDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 21));
            assertThat(fellesperiode.getFellesperiodeTilOgMedDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 31));
            assertThat(fellesperiode.getSkalDenAndreForelderenHaForeldrepengerISammePeriode1()).isEqualTo(JaNei.NEI);
            assertThat(fellesperiode.getSkalDuKombinereForeldrepengeneMedDelvisArbeid()).isEqualTo(JaNei.JA);
            assertThat(fellesperiode.getOppgiStillingsprosentenDuSkalJobbe()).isEqualTo(55);

            assertThat(fellesperiode.getHvorSkalDuJobbe()).isNotNull();
            assertThat(fellesperiode.getHvorSkalDuJobbe().isHosArbeidsgiver()).isTrue();
            assertThat(fellesperiode.getHvorSkalDuJobbe().isFrilanser()).isFalse();
            assertThat(fellesperiode.getHvorSkalDuJobbe().isSelvstendigNaeringsdrivende()).isFalse();
            assertThat(fellesperiode.getNavnPaArbeidsgiver()).isEqualTo("yhjkl");

            // Verify attachments
            assertThat(submission.getData().getAttachments()).isNotNull();
            assertThat(submission.getData().getAttachments()).hasSize(3);

            var personalIdAttachment = submission.getData().getAttachments().get(0);
            assertThat(personalIdAttachment.getType()).isEqualTo("personal-id");
            assertThat(personalIdAttachment.getValue()).isEqualTo("norwegianPassport");
            assertThat(personalIdAttachment.getTitle()).isEqualTo("Norsk pass");
            assertThat(personalIdAttachment.getFiles()).hasSize(1);
            assertThat(personalIdAttachment.getFiles().get(0).getFileName()).isEqualTo("delme-4 (1).pdf");
            assertThat(personalIdAttachment.getFiles().get(0).getSize()).isEqualTo(1170049L);
        }
    }
}
