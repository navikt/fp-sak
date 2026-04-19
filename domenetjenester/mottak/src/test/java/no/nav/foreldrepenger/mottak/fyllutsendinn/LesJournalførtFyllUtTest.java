package no.nav.foreldrepenger.mottak.fyllutsendinn;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class LesJournalførtFyllUtTest {

    @Test
    void les_engangsstonad_fodsel_eksempel140507_termin() throws Exception {
        // eksempel140507-termin.json - Engangsstønad ved fødsel (fremtidig fødsel)
        try (var inputStream = getClass().getResourceAsStream("/fyllutsendinn/eksempel140507-termin.json")) {
            FormSubmission<Nav140507Data> submission = DefaultJsonMapper.getJsonMapper()
                .readerFor(new TypeReference<FormSubmission<Nav140507Data>>() {})
                .readValue(inputStream);

            // Verify top-level structure
            assertThat(submission).isNotNull();
            assertThat(submission.language()).isEqualTo("nb");
            assertThat(submission.data()).isNotNull();
            assertThat(submission.data().data()).isNotNull();

            // Verify data is correct type
            var data = submission.data().data();
            assertThat(data).isInstanceOf(Nav140507Data.class);

            // Verify confirmations
            assertThat(data.jegHarLestOgForstattDetSomStarPaNavNoRettogplikt()).isTrue();
            assertThat(data.deOpplysningerJegHarOppgittErRiktigeOgJegHarIkkeHoldtTilbakeOpplysningerSomHarBetydningForMinRettTilEngangsstonad()).isTrue();

            // Verify personal information
            assertThat(data.fornavnSoker()).isEqualTo("Iherdig");
            assertThat(data.etternavnSoker()).isEqualTo("Hestedrosje");
            assertThat(data.fodselsnummerDNummerSoker()).isEqualTo("08480594097");

            // Verify what is being applied for
            assertThat(data.hvaSokerDuOm()).isEqualTo(Nav140507Data.HvaSokerDuOm.ENGANGSSTONAD_VED_FODSEL);
            assertThat(data.narErBarnetFodt()).isEqualTo(Nav140507Data.NarErBarnetFodt.FREM_I_TID);
            assertThat(data.antallBarn()).isEqualTo(1);
            assertThat(data.termindatoDdMmAaaa()).isEqualTo(LocalDate.of(2026, 5, 17));

            // Verify optional lists absent from JSON
            assertThat(data.leggTilBarnetEllerBarnasFodselsdato()).isNull();
            assertThat(data.utenlandsopphold()).isNull();
            assertThat(data.utenlandsopphold1()).isNull();

            // Verify residence information
            assertThat(data.planleggerDuAVaereINorgePaFodselstidspunktet1()).isEqualTo(JaNei.JA);
            assertThat(data.hvorSkalDuBoDeNeste12Manedene()).isEqualTo(Nav140507Data.HvorSkalDuBoDeNeste12Manedene.KUN_BO_I_NORGE);
            assertThat(data.hvorHarDuBoddDeSiste12Manedene()).isEqualTo(Nav140507Data.HvorHarDuBoddDeSiste12Manedene.KUN_BODD_I_NORGE);
            assertThat(data.harDuTilleggsopplysningerSomErRelevantForSoknaden()).isEqualTo(JaNei.NEI);

            // Verify attachments
            assertThat(submission.data().attachments()).isNotNull();
            assertThat(submission.data().attachments()).hasSize(3);

            var personalIdAttachment = submission.data().attachments().getFirst();
            assertThat(personalIdAttachment.attachmentId()).isEqualTo("personal-id");
            assertThat(personalIdAttachment.type()).isEqualTo("personal-id");
            assertThat(personalIdAttachment.value()).isEqualTo("driversLicense");
            assertThat(personalIdAttachment.title()).isEqualTo("Norsk førerkort utstedt fra og med 01.01.1998");
            assertThat(personalIdAttachment.files()).hasSize(1);
            assertThat(personalIdAttachment.files().getFirst().fileName()).isEqualTo("microsystems.png");
            assertThat(personalIdAttachment.files().getFirst().size()).isEqualTo(29263L);

            var secondAttachment = submission.data().attachments().get(1);
            assertThat(secondAttachment.attachmentId()).isEqualTo("ev6045b");
            assertThat(secondAttachment.type()).isEqualTo("default");
            assertThat(secondAttachment.value()).isEqualTo("leggerVedNaa");
            assertThat(secondAttachment.files()).hasSize(1);
            assertThat(secondAttachment.files().getFirst().fileName()).isEqualTo("Screenshot 2025-10-31 at 11.27.58.png");
            assertThat(secondAttachment.files().getFirst().size()).isEqualTo(89099L);

            var thirdAttachment = submission.data().attachments().get(2);
            assertThat(thirdAttachment.attachmentId()).isEqualTo("es3i5y9h");
            assertThat(thirdAttachment.type()).isEqualTo("other");
            assertThat(thirdAttachment.value()).isEqualTo("nei");
            assertThat(thirdAttachment.files()).isEmpty();
        }
    }

    // TODO - avsjekk med FUtSInn om landvelger - alle er "string", men det blir generert objekt
    //           "hvilketLandBoddeDuI": { "value": "SE", "label": "Sverige" },
    // Hvis object isf string - Endre i 07-fodsel: "hvilketLandBoddeDuI": "SE",
    @Test
    void les_engangsstonad_fodsel_eksempel140507_fodsel() throws Exception {
        // eksempel140507-fodsel.json - Engangsstønad ved fødsel (allerede født, utenlandsopphold)
        try (var inputStream = getClass().getResourceAsStream("/fyllutsendinn/eksempel140507-fodsel.json")) {
            FormSubmission<Nav140507Data> submission = DefaultJsonMapper.getJsonMapper()
                .readerFor(new TypeReference<FormSubmission<Nav140507Data>>() {})
                .readValue(inputStream);

            // Verify top-level structure
            assertThat(submission).isNotNull();
            assertThat(submission.language()).isEqualTo("nb");
            assertThat(submission.data()).isNotNull();
            assertThat(submission.data().data()).isNotNull();

            // Verify data is correct type
            var data = submission.data().data();
            assertThat(data).isInstanceOf(Nav140507Data.class);

            // Verify confirmations
            assertThat(data.jegHarLestOgForstattDetSomStarPaNavNoRettogplikt()).isTrue();
            assertThat(data.deOpplysningerJegHarOppgittErRiktigeOgJegHarIkkeHoldtTilbakeOpplysningerSomHarBetydningForMinRettTilEngangsstonad()).isTrue();

            // Verify personal information
            assertThat(data.fornavnSoker()).isEqualTo("Tørr");
            assertThat(data.etternavnSoker()).isEqualTo("Telegram");
            assertThat(data.fodselsnummerDNummerSoker()).isEqualTo("04410577041");

            // Verify what is being applied for
            assertThat(data.hvaSokerDuOm()).isEqualTo(Nav140507Data.HvaSokerDuOm.ENGANGSSTONAD_VED_FODSEL);
            assertThat(data.narErBarnetFodt()).isEqualTo(Nav140507Data.NarErBarnetFodt.TILBAKE_I_TID);
            assertThat(data.antallBarn()).isEqualTo(2);
            assertThat(data.termindatoDdMmAaaa()).isNull();

            // Verify birth dates list
            assertThat(data.leggTilBarnetEllerBarnasFodselsdato()).hasSize(1);
            assertThat(data.leggTilBarnetEllerBarnasFodselsdato().getFirst().fodselsdatoDdMmAaaa()).isEqualTo(LocalDate.of(2026, 4, 1));

            // Verify residence information
            assertThat(data.planleggerDuAVaereINorgePaFodselstidspunktet1()).isEqualTo(JaNei.JA);
            assertThat(data.hvorSkalDuBoDeNeste12Manedene()).isEqualTo(Nav140507Data.HvorSkalDuBoDeNeste12Manedene.KUN_BO_I_NORGE);
            assertThat(data.hvorHarDuBoddDeSiste12Manedene()).isEqualTo(Nav140507Data.HvorHarDuBoddDeSiste12Manedene.BODD_I_UTLANDET_HELT_ELLER_DELVIS);
            assertThat(data.utenlandsopphold()).isNull();

            // Verify previous foreign stay
            assertThat(data.utenlandsopphold1()).hasSize(1);
            var utenlandsopphold = data.utenlandsopphold1().getFirst();
            assertThat(utenlandsopphold.fraDatoDdMmAaaa()).isEqualTo(LocalDate.of(2025, 12, 1));
            assertThat(utenlandsopphold.tilDatoDdMmAaaa()).isEqualTo(LocalDate.of(2025, 12, 31));

            assertThat(data.harDuTilleggsopplysningerSomErRelevantForSoknaden()).isEqualTo(JaNei.NEI);

            // Verify attachments
            assertThat(submission.data().attachments()).isNotNull();
            assertThat(submission.data().attachments()).hasSize(2);

            var personalIdAttachment = submission.data().attachments().getFirst();
            assertThat(personalIdAttachment.attachmentId()).isEqualTo("personal-id");
            assertThat(personalIdAttachment.type()).isEqualTo("personal-id");
            assertThat(personalIdAttachment.value()).isEqualTo("nationalIdEU");
            assertThat(personalIdAttachment.title()).isEqualTo("Nasjonalt ID-kort fra EU/EØS-land og Sveits");
            assertThat(personalIdAttachment.files()).hasSize(1);
            assertThat(personalIdAttachment.files().getFirst().fileName()).isEqualTo("Screenshot 2025-10-31 at 11.24.04.png");
            assertThat(personalIdAttachment.files().getFirst().size()).isEqualTo(73409L);

            var secondAttachment = submission.data().attachments().get(1);
            assertThat(secondAttachment.attachmentId()).isEqualTo("es3i5y9h");
            assertThat(secondAttachment.type()).isEqualTo("other");
            assertThat(secondAttachment.value()).isEqualTo("nei");
            assertThat(secondAttachment.files()).isEmpty();
        }
    }

    @Test
    void les_svangerskapspenger_eksempel140410() throws Exception {
        // eksempel140410.json - Svangerskapspenger søknad
        try (var inputStream = getClass().getResourceAsStream("/fyllutsendinn/eksempel140410.json")) {
            FormSubmission<Nav140410Data> submission = DefaultJsonMapper.getJsonMapper()
                .readerFor(new TypeReference<FormSubmission<Nav140410Data>>() {})
                .readValue(inputStream);

            // Verify top-level structure
            assertThat(submission).isNotNull();
            assertThat(submission.language()).isEqualTo("nb");
            assertThat(submission.data()).isNotNull();
            assertThat(submission.data().data()).isNotNull();

            // Verify data is correct type
            var data = submission.data().data();
            assertThat(data).isInstanceOf(Nav140410Data.class);

            // Verify personal information
            assertThat(data.dineOpplysninger1()).isNotNull();
            assertThat(data.dineOpplysninger1().fornavn()).isEqualTo("hjk");
            assertThat(data.dineOpplysninger1().etternavn()).isEqualTo("Kåre");
            assertThat(data.dineOpplysninger1().identitet()).isNotNull();
            assertThat(data.dineOpplysninger1().identitet().harDuFodselsnummer()).isEqualTo(JaNei.JA);
            assertThat(data.dineOpplysninger1().identitet().identitetsnummer()).isEqualTo("21831999753");

            // Verify svangerskapspenger specific fields
            assertThat(data.harDuBoddINorgeDeSiste12Manedene()).isEqualTo(Nav140410Data.HarDuBoddINorgeDeSiste12Manedene.JEG_HAR_KUN_BODD_I_NORGE);
            assertThat(data.hvorSkalDuBoDeNeste12Manedene()).isEqualTo(Nav140410Data.HvorSkalDuBoDeNeste12Manedene.JEG_SKAL_KUN_BO_I_NORGE);
            assertThat(data.harDuHattJobbIEuEosLandDeSiste10Manedene()).isEqualTo(JaNei.JA);
            assertThat(data.hvilketLandJobbetDuI()).isEqualTo(Nav140410Data.HvilketLandJobbetDuI.ESTLAND);
            assertThat(data.oppgiNavnetPaArbeidsgiveren()).isEqualTo("fghjk");
            assertThat(data.harDuJobbetOgHattInntektSomFrilanserDeSiste10Manedene()).isEqualTo(JaNei.NEI);
            assertThat(data.harDuJobbetOgHattInntektSomSelvstendigNaeringsdrivendeDeSiste10Manedene()).isEqualTo(JaNei.NEI);

            // Verify dates
            assertThat(data.narHarDuTermindatoDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 26));
            assertThat(data.erBarnetFodt()).isEqualTo(JaNei.JA);
            assertThat(data.narBleBarnetFodtDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 17));
            assertThat(data.fraHvilkenDatoHarDuBehovForSvangerskapspengerDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 10));
            assertThat(data.fraHvilkenDatoHarDuHattJobbIEuEosLandDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 4));
            assertThat(data.erDetEnJobbDuHarPerIDag()).isEqualTo(JaNei.NEI);
            assertThat(data.tilHvilkenDatoHarDuHattJobbIEuEosLandDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 19));

            // Verify work situation
            assertThat(data.hvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger()).isNotNull();
            assertThat(data.hvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger().jegKanFortsetteMedSammeStillingsprosent()).isTrue();
            assertThat(data.hvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger().jegKanFortsetteMedRedusertArbeidstid()).isFalse();
            assertThat(data.hvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger().jegKanIkkeFortsetteAJobbe()).isFalse();

            // Verify attachments
            assertThat(submission.data().attachments()).isNotNull();
            assertThat(submission.data().attachments()).hasSize(3);

            var personalIdAttachment = submission.data().attachments().getFirst();
            assertThat(personalIdAttachment.attachmentId()).isEqualTo("personal-id");
            assertThat(personalIdAttachment.type()).isEqualTo("personal-id");
            assertThat(personalIdAttachment.value()).isEqualTo("norwegianPassport");
            assertThat(personalIdAttachment.files()).hasSize(1);
            assertThat(personalIdAttachment.files().getFirst().fileName()).isEqualTo("delme-4 (1).pdf");
            assertThat(personalIdAttachment.files().getFirst().size()).isEqualTo(1170049L);
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
            assertThat(submission.language()).isEqualTo("nb");
            assertThat(submission.data()).isNotNull();
            assertThat(submission.data().data()).isNotNull();

            // Verify data is correct type
            var data = submission.data().data();
            assertThat(data).isInstanceOf(Nav140509Data.class);

            // Verify confirmations
            assertThat(data.jegVilSvareSaGodtJegKanPaSporsmaleneISoknaden()).isTrue();
            assertThat(data.jegVilSvareSaGodtJegKanPaSporsmaleneISoknaden()).isTrue();

            // Verify personal information
            assertThat(data.dineOpplysninger1()).isNotNull();
            assertThat(data.dineOpplysninger1().fornavn()).isEqualTo("Bob");
            assertThat(data.dineOpplysninger1().etternavn()).isEqualTo("Kåre");
            assertThat(data.dineOpplysninger1().identitet()).isNotNull();
            assertThat(data.dineOpplysninger1().identitet().harDuFodselsnummer()).isEqualTo(JaNei.JA);
            assertThat(data.dineOpplysninger1().identitet().identitetsnummer()).isEqualTo("21831999753");

            // Verify applicant role and benefit type
            assertThat(data.hvemErDu()).isEqualTo(Nav140509Data.HvemErDu.FAR);
            assertThat(data.hvorLangPeriodeMedForeldrepengerOnskerDu()).isEqualTo(Nav140509Data.HvorLangPeriodeMedForeldrepengerOnskerDu._100_PROSENT_FORELDREPENGER);

            // Verify birth information
            assertThat(data.erBarnetFodt()).isEqualTo(JaNei.JA);
            assertThat(data.barnetErFodt()).isNotNull();
            assertThat(data.barnetErFodt().hvorMangeBarnFikkDu()).isEqualTo(2);
            assertThat(data.barnetErFodt().narBleDetEldsteBarnetFodtDdMmAaaa1()).isEqualTo(LocalDate.of(2026, 3, 2));
            assertThat(data.barnetErFodt().narVarTermindatoDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 2));
            assertThat(data.barnetErFodt().bleBarnaFodtINorge()).isEqualTo(JaNei.JA);

            // Verify other parent information (aleneomsorg)
            assertThat(data.kanDuGiOssNavnetPaDenAndreForelderen()).isEqualTo(JaNei.NEI);

            // Verify benefit period
            assertThat(data.periodeMedforeldrepengerVedAleneomsorgFarMedmor()).hasSize(1);
            var periode = data.periodeMedforeldrepengerVedAleneomsorgFarMedmor().getFirst();
            assertThat(periode.datoFraOgMedDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 2));
            assertThat(periode.datoTilOgMedDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 13));
            assertThat(periode.skalDuKombinereForeldrepengeneMedDelvisArbeid()).isEqualTo(JaNei.NEI);

            // Verify residence information
            assertThat(data.hvorSkalDuBoDeNeste12Manedene()).isEqualTo(Nav140509Data.HvorSkalDuBoDeNeste12Manedene.KUN_BO_I_NORGE);
            assertThat(data.hvorHarDuBoddDeSiste12Manedene()).isEqualTo(Nav140509Data.HvorHarDuBoddDeSiste12Manedene.KUN_BODD_I_NORGE);

            // Verify income sources
            assertThat(data.harDuArbeidsforholdINorge()).isEqualTo(JaNei.NEI);
            assertThat(data.harDuJobbetOgHattInntektSomFrilanserDeSiste10Manedene()).isEqualTo(JaNei.NEI);
            assertThat(data.harDuJobbetOgHattInntektSomSelvstendigNaeringsdrivendeDeSiste10Manedene()).isEqualTo(JaNei.NEI);

            // Verify attachments
            assertThat(submission.data().attachments()).isNotNull();
            assertThat(submission.data().attachments()).hasSize(3);

            var firstAttachment = submission.data().attachments().getFirst();
            assertThat(firstAttachment.type()).isEqualTo("personal-id");
            assertThat(firstAttachment.value()).isEqualTo("norwegianPassport");
            assertThat(firstAttachment.files()).hasSize(1);
            assertThat(firstAttachment.files().getFirst().fileName()).isEqualTo("delme-3 (1).pdf");
            assertThat(firstAttachment.files().getFirst().size()).isEqualTo(385544L);
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
            assertThat(submission.language()).isEqualTo("nb");
            assertThat(submission.data()).isNotNull();
            assertThat(submission.data().data()).isNotNull();

            // Verify data is correct type
            var data = submission.data().data();
            assertThat(data).isInstanceOf(Nav141605Data.class);

            // Verify confirmations
            assertThat(data.jegVilSvareSaGodtJegKanPaSporsmaleneISoknaden2()).isTrue();
            assertThat(data.jegBekrefterAtjegSkalHaOmsorgenForBarnetIPeriodeneJegSokerForeldrepenger()).isTrue();

            // Verify personal information
            assertThat(data.dineOpplysninger1()).isNotNull();
            assertThat(data.dineOpplysninger1().fornavn()).isEqualTo("Bob");
            assertThat(data.dineOpplysninger1().etternavn()).isEqualTo("Kåre");
            assertThat(data.dineOpplysninger1().identitet()).isNotNull();
            assertThat(data.dineOpplysninger1().identitet().harDuFodselsnummer()).isEqualTo(JaNei.JA);
            assertThat(data.dineOpplysninger1().identitet().identitetsnummer()).isEqualTo("21831999753");

            // Verify applicant role
            assertThat(data.hvemErDu()).isEqualTo(Nav141605Data.HvemErDu.MOR);
            assertThat(data.erDuAleneOmOmsorgenAvBarnet()).isEqualTo(JaNei.NEI);

            // Verify what is being applied for
            assertThat(data.hvaSokerDuOm()).isNotNull();
            assertThat(data.hvaSokerDuOm().periodeMedForeldrepenger()).isTrue();
            assertThat(data.hvaSokerDuOm().periodeUtenForeldrepenger()).isFalse();
            assertThat(data.hvaSokerDuOm().utsettelseForste6UkeneEtterFodsel()).isTrue();

            // Verify benefit periods for mor
            assertThat(data.mor()).isNotNull();
            assertThat(data.mor().hvilkenPeriodeSkalDuTaUtMor()).isNotNull();
            var hvilkenPeriode = data.mor().hvilkenPeriodeSkalDuTaUtMor();
            assertThat(hvilkenPeriode.foreldrepengerForFodsel()).isTrue();
            assertThat(hvilkenPeriode.modrekvote()).isFalse();
            assertThat(hvilkenPeriode.fellesperiode()).isTrue();
            assertThat(hvilkenPeriode.overforingAvAnnenForeldersKvote()).isFalse();

            // Verify other parent information
            assertThat(data.harDenAndreForelderenRettTilForeldrepenger()).isEqualTo(JaNei.JA);
            assertThat(data.harDuOrientertDenAndreForelderenOmSoknadenDin()).isEqualTo(JaNei.JA);

            // Verify utsettelse periods
            assertThat(data.perioderMedUtsettelseForste6UkerEtterFodsel()).hasSize(1);
            var utsettelse = data.perioderMedUtsettelseForste6UkerEtterFodsel().getFirst();
            assertThat(utsettelse.datoFraOgMedDdMmAaaa1()).isEqualTo(LocalDate.of(2026, 3, 10));
            assertThat(utsettelse.datoTilOgMedDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 27));
            assertThat(utsettelse.hvorforSkalDuUtsetteForeldrepenger()).isEqualTo(Nav141605Data.HvorforSkalDuUtsetteForeldrepenger.JEG_ER_FOR_SYK_TIL_A_TA_MEG_AV_BARNET);

            // Verify foreldrepenger før fødsel
            assertThat(data.foreldrepengerForFodsel()).hasSize(1);
            var forFodselPeriode = data.foreldrepengerForFodsel().getFirst();
            assertThat(forFodselPeriode.foreldrepengerFraOgMedDatoDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 3));
            assertThat(forFodselPeriode.foreldrepengerTilOgMedDatoDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 26));

            // Verify fellesperiode with gradering
            assertThat(data.fellesperiodeMor()).hasSize(1);
            var fellesperiode = data.fellesperiodeMor().getFirst();
            assertThat(fellesperiode.fellesperiodeFraOgMedDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 21));
            assertThat(fellesperiode.fellesperiodeTilOgMedDdMmAaaa()).isEqualTo(LocalDate.of(2026, 3, 31));
            assertThat(fellesperiode.skalDenAndreForelderenHaForeldrepengerISammePeriode1()).isEqualTo(JaNei.NEI);
            assertThat(fellesperiode.skalDuKombinereForeldrepengeneMedDelvisArbeid()).isEqualTo(JaNei.JA);
            assertThat(fellesperiode.oppgiStillingsprosentenDuSkalJobbe()).isEqualTo(55);

            assertThat(fellesperiode.hvorSkalDuJobbe()).isNotNull();
            assertThat(fellesperiode.hvorSkalDuJobbe().hosArbeidsgiver()).isTrue();
            assertThat(fellesperiode.hvorSkalDuJobbe().frilanser()).isFalse();
            assertThat(fellesperiode.hvorSkalDuJobbe().selvstendigNaeringsdrivende()).isFalse();
            assertThat(fellesperiode.navnPaArbeidsgiver()).isEqualTo("yhjkl");

            // Verify attachments
            assertThat(submission.data().attachments()).isNotNull();
            assertThat(submission.data().attachments()).hasSize(3);

            var personalIdAttachment = submission.data().attachments().getFirst();
            assertThat(personalIdAttachment.type()).isEqualTo("personal-id");
            assertThat(personalIdAttachment.value()).isEqualTo("norwegianPassport");
            assertThat(personalIdAttachment.title()).isEqualTo("Norsk pass");
            assertThat(personalIdAttachment.files()).hasSize(1);
            assertThat(personalIdAttachment.files().getFirst().fileName()).isEqualTo("delme-4 (1).pdf");
            assertThat(personalIdAttachment.files().getFirst().size()).isEqualTo(1170049L);
        }
    }
}
