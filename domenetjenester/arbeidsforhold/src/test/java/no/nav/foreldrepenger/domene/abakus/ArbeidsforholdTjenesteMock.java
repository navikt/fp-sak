package no.nav.foreldrepenger.domene.abakus;

import no.nav.abakus.iaygrunnlag.ArbeidsforholdRefDto;
import no.nav.abakus.iaygrunnlag.Organisasjon;
import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.ArbeidsforholdDto;
import no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType;
import no.nav.vedtak.konfig.Tid;

import java.time.LocalDate;
import java.util.List;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArbeidsforholdTjenesteMock {

    private static final String ORGNR1 = KUNSTIG_ORG;
    private static final String ORGNR2 = "52";
    private static final LocalDate PERIODE_FOM = LocalDate.now().minusYears(3L);
    private final ArbeidsforholdTjeneste arbeidsforholdTjeneste;

    public ArbeidsforholdTjenesteMock(boolean medToArbeidsforhold) {
        var response = opprettResponse(medToArbeidsforhold);

        var arbeidsforholdConsumer = mock(AbakusTjeneste.class);
        when(arbeidsforholdConsumer.hentArbeidsforholdIPerioden(any())).thenReturn(response);
        this.arbeidsforholdTjeneste = new ArbeidsforholdTjeneste(arbeidsforholdConsumer);
    }

    public ArbeidsforholdTjeneste getMock() {
        return arbeidsforholdTjeneste;
    }

    private List<ArbeidsforholdDto> opprettResponse(boolean medToArbeidsforhold) {
        var arbeidsforhold = new ArbeidsforholdDto(new Organisasjon(ORGNR1), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        arbeidsforhold.setAnsettelsesperiode(List.of(new Periode(PERIODE_FOM, Tid.TIDENES_ENDE)));
        arbeidsforhold.setArbeidsforholdId(new ArbeidsforholdRefDto(null, "1"));

        if (medToArbeidsforhold) {
            var arbeidsforhold2 = new ArbeidsforholdDto(new Organisasjon(ORGNR2), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
            arbeidsforhold2.setArbeidsforholdId(new ArbeidsforholdRefDto(null, "1"));
            arbeidsforhold2.setAnsettelsesperiode(List.of(new Periode(PERIODE_FOM, Tid.TIDENES_ENDE)));
            return List.of(arbeidsforhold, arbeidsforhold2);
        }
        return List.of(arbeidsforhold);
    }
}
