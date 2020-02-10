package no.nav.foreldrepenger.domene.iay.modell;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;

public class ArbeidsforholdInformasjonEntitetTest {

    @Test
    public void skal_beholde_referanse_til() {
        var virksomhet1 = new VirksomhetEntitet.Builder().medOrgnr("1234").build();
        var virksomhet2 = new VirksomhetEntitet.Builder().medOrgnr("5678").build();
        var arbeidsgiver1 = Arbeidsgiver.fra(virksomhet1);
        var arbeidsgiver2 = Arbeidsgiver.fra(virksomhet2);
        var entitet = new ArbeidsforholdInformasjon();

        var ref1 = EksternArbeidsforholdRef.ref("1234");
        var ref2 = EksternArbeidsforholdRef.ref("5678");
        entitet.finnEllerOpprett(arbeidsgiver1, ref1);
        entitet.finnEllerOpprett(arbeidsgiver1, ref2);
        entitet.finnEllerOpprett(arbeidsgiver2, ref1);
        entitet.finnEllerOpprett(arbeidsgiver2, ref2);
        var ref_1_2 = entitet.finnForEkstern(arbeidsgiver1, ref2).get();
        var ref_1_1 = entitet.finnForEkstern(arbeidsgiver1, ref1).get();
        entitet.erstattArbeidsforhold(arbeidsgiver1, ref_1_1, ref_1_2);

        var overstyringBuilderFor = entitet.getOverstyringBuilderFor(arbeidsgiver1, ref_1_1);
        overstyringBuilderFor.medNyArbeidsforholdRef(ref_1_2)
            .medHandling(ArbeidsforholdHandlingType.SLÅTT_SAMMEN_MED_ANNET)
            .medBeskrivelse("asdf");
        entitet.leggTilOverstyring(overstyringBuilderFor.build());

        assertThat(entitet.finnForEkstern(arbeidsgiver1, ref1)).isNotEqualTo(ref1);
        assertThat(entitet.finnForEkstern(arbeidsgiver1, ref2)).isNotEqualTo(ref2);
        assertThat(entitet.finnForEkstern(arbeidsgiver1, ref1)).isEqualTo(entitet.finnForEkstern(arbeidsgiver1, ref2));
        assertThat(entitet.finnForEksternBeholdHistoriskReferanse(arbeidsgiver1, ref1)).isNotEqualTo(entitet.finnForEkstern(arbeidsgiver1, ref2));
        assertThat(entitet.finnEllerOpprett(arbeidsgiver1, ref1)).isEqualTo(entitet.finnEllerOpprett(arbeidsgiver1, ref2));
        assertThat(entitet.finnForEkstern(arbeidsgiver1, ref1)).isEqualTo(entitet.finnForEksternBeholdHistoriskReferanse(arbeidsgiver1, ref2));
        assertThat(entitet.finnForEkstern(arbeidsgiver2, ref1)).isNotEqualTo(entitet.finnForEkstern(arbeidsgiver2, ref2));
    }
}
