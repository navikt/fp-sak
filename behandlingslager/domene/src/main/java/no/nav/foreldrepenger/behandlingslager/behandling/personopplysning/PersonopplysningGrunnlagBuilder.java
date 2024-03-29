package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import java.util.Optional;

public class PersonopplysningGrunnlagBuilder {

    private final PersonopplysningGrunnlagEntitet kladd;

    private PersonopplysningGrunnlagBuilder(PersonopplysningGrunnlagEntitet kladd) {
        this.kladd = kladd;
    }

    private static PersonopplysningGrunnlagBuilder nytt() {
        return new PersonopplysningGrunnlagBuilder(new PersonopplysningGrunnlagEntitet());
    }

    private static PersonopplysningGrunnlagBuilder oppdatere(PersonopplysningGrunnlagEntitet kladd) {
        return new PersonopplysningGrunnlagBuilder(new PersonopplysningGrunnlagEntitet(kladd));
    }

    public static PersonopplysningGrunnlagBuilder oppdatere(Optional<PersonopplysningGrunnlagEntitet> kladd) {
        return kladd.map(PersonopplysningGrunnlagBuilder::oppdatere).orElseGet(PersonopplysningGrunnlagBuilder::nytt);
    }

    public PersonopplysningGrunnlagBuilder medRegistrertVersjon(PersonInformasjonBuilder builder) {
        if (builder.getType().equals(PersonopplysningVersjonType.REGISTRERT)
                && kladd.getRegisterVersjon().isPresent() == builder.gjelderOppdatering()) {
            kladd.setRegistrertePersonopplysninger(builder.build());
        } else {
            throw PersonopplysningFeil.måBasereSegPåEksisterendeVersjon();
        }
        return this;
    }

    public PersonopplysningGrunnlagBuilder medOverstyrtVersjon(PersonInformasjonBuilder builder) {
        if (builder == null) {
            kladd.setOverstyrtePersonopplysninger(null);
        } else if (builder.getType().equals(PersonopplysningVersjonType.OVERSTYRT)
                && kladd.getOverstyrtVersjon().isPresent() == builder.gjelderOppdatering()) {
            kladd.setOverstyrtePersonopplysninger(builder.build());
        } else {
            throw PersonopplysningFeil.måBasereSegPåEksisterendeVersjon();
        }
        return this;
    }

    public PersonopplysningGrunnlagBuilder medOppgittAnnenPart(OppgittAnnenPartEntitet oppgittAnnenPart) {
        kladd.setOppgittAnnenPart(oppgittAnnenPart);
        return this;
    }

    public PersonopplysningGrunnlagEntitet build() {
        return kladd;
    }

}
